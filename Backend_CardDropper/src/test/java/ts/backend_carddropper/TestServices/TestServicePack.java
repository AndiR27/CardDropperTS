package ts.backend_carddropper.TestServices;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ts.backend_carddropper.entity.Card;
import ts.backend_carddropper.entity.PackSlot;
import ts.backend_carddropper.entity.PackTemplate;
import ts.backend_carddropper.entity.User;
import ts.backend_carddropper.enums.Rarity;
import ts.backend_carddropper.helper.TestDataHelper;
import ts.backend_carddropper.models.CardDto;
import ts.backend_carddropper.repository.RepositoryCard;
import ts.backend_carddropper.repository.RepositoryPackTemplate;
import ts.backend_carddropper.repository.RepositoryUser;
import ts.backend_carddropper.service.ServicePack;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class TestServicePack {

    @Autowired
    private ServicePack servicePack;

    @Autowired
    private TestDataHelper testDataHelper;

    @MockitoBean
    private RepositoryPackTemplate repositoryPackTemplate;

    @MockitoBean
    private RepositoryCard repositoryCard;

    @MockitoBean
    private RepositoryUser repositoryUser;

    private User alice;
    private List<Card> cards;
    private PackTemplate packTemplate;

    @BeforeEach
    void setUp() {
        List<User> users = testDataHelper.createUsers();
        alice = users.getFirst();
        cards = testDataHelper.createCards(alice);
        packTemplate = testDataHelper.createPackTemplate();
    }

    /**
     * Mocks all pool lookups (both with and without exclusions) for every rarity.
     */
    private void mockAllPools() {
        for (Rarity rarity : Rarity.values()) {
            List<Card> pool = cards.stream().filter(c -> c.getRarity() == rarity).toList();
            when(repositoryCard.findByRarityAndUserIsNull(rarity)).thenReturn(pool);
            when(repositoryCard.findByRarityAndUserIsNullAndIdNotIn(eq(rarity), anyList()))
                    .thenAnswer(inv -> {
                        List<Long> excluded = inv.getArgument(1);
                        return pool.stream().filter(c -> !excluded.contains(c.getId())).toList();
                    });
        }
    }

    /**
     * Mocks the common infrastructure needed for a full generatePack call (saveAll, findAllById, user lookup).
     */
    private void mockPackInfrastructure() {
        when(repositoryCard.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
        when(repositoryCard.findAllById(anyList())).thenAnswer(inv -> {
            List<Long> ids = inv.getArgument(0);
            return cards.stream().filter(c -> ids.contains(c.getId())).toList();
        });
        when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
    }


    // ========================================
    //         GENERATE PACK TESTS
    // ========================================

    @Nested
    @DisplayName("generatePack")
    class GeneratePackTests {

        @Test
        @DisplayName("generates pack with correct number of cards (1 per slot)")
        void testGeneratePack_success() {
            when(repositoryPackTemplate.findById(packTemplate.getId())).thenReturn(Optional.of(packTemplate));
            mockAllPools();
            mockPackInfrastructure();

            List<CardDto> result = servicePack.generatePack(alice.getId(), packTemplate.getId());

            assertNotNull(result);
            assertEquals(3, result.size(), "Pack should contain 3 cards (one per slot)");
        }

        @Test
        @DisplayName("generates pack and all cards are assigned to user")
        void testGeneratePack_cardsAssignedToUser() {
            when(repositoryPackTemplate.findById(packTemplate.getId())).thenReturn(Optional.of(packTemplate));
            mockAllPools();
            mockPackInfrastructure();

            List<CardDto> result = servicePack.generatePack(alice.getId(), packTemplate.getId());

            // After openPack, all returned cards should have userId = alice
            for (CardDto dto : result) {
                assertEquals(alice.getId(), dto.userId());
            }
        }

        @Test
        @DisplayName("throws when template not found")
        void testGeneratePack_templateNotFound() {
            when(repositoryPackTemplate.findById(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> servicePack.generatePack(alice.getId(), 999L));
        }

        @Test
        @DisplayName("throws when pool is empty for a required rarity")
        void testGeneratePack_emptyPool() {
            // Template with a single fixed LENGENDARY slot
            PackTemplate legendaryTemplate = new PackTemplate();
            legendaryTemplate.setId(10L);
            legendaryTemplate.setName("Legendary Only");

            PackSlot slot = new PackSlot();
            slot.setId(10L);
            slot.setPackTemplate(legendaryTemplate);
            slot.setFixedRarity(Rarity.LEGENDARY);
            legendaryTemplate.setSlots(List.of(slot));

            when(repositoryPackTemplate.findById(legendaryTemplate.getId())).thenReturn(Optional.of(legendaryTemplate));
            // Empty pool for LEGENDARY
            when(repositoryCard.findByRarityAndUserIsNull(Rarity.LEGENDARY)).thenReturn(Collections.emptyList());

            assertThrows(IllegalStateException.class,
                    () -> servicePack.generatePack(alice.getId(), legendaryTemplate.getId()));
        }
    }


    // ========================================
    //         DROP RATE TESTS
    // ========================================

    @Nested
    @DisplayName("Drop rate mechanics")
    class DropRateTests {

        @Test
        @DisplayName("non-unique cards get their dropRate reduced after being picked")
        void testDropRateReduced_nonUnique() {
            // Single-slot template to control exactly which card is picked
            PackTemplate singleSlot = new PackTemplate();
            singleSlot.setId(20L);
            singleSlot.setName("Single Slot");

            PackSlot slot = new PackSlot();
            slot.setId(20L);
            slot.setPackTemplate(singleSlot);
            slot.setFixedRarity(Rarity.COMMON);
            singleSlot.setSlots(List.of(slot));

            // Single card in pool — non-unique, dropRate 1.0
            Card poolCard = new Card();
            poolCard.setId(50L);
            poolCard.setName("common_test");
            poolCard.setRarity(Rarity.COMMON);
            poolCard.setDropRate(1.0);
            poolCard.setUniqueCard(false);
            poolCard.setCreatedBy(alice);

            double originalDropRate = poolCard.getDropRate();

            when(repositoryPackTemplate.findById(singleSlot.getId())).thenReturn(Optional.of(singleSlot));
            when(repositoryCard.findByRarityAndUserIsNull(Rarity.COMMON)).thenReturn(List.of(poolCard));
            when(repositoryCard.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
            when(repositoryCard.findAllById(anyList())).thenReturn(List.of(poolCard));
            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));

            servicePack.generatePack(alice.getId(), singleSlot.getId());

            // dropRate should have been reduced: 1.0 * (1 - 0.05) = 0.95
            assertTrue(poolCard.getDropRate() < originalDropRate);
            assertEquals(0.95, poolCard.getDropRate(), 0.001);
        }

        @Test
        @DisplayName("unique cards keep their dropRate unchanged")
        void testDropRateUnchanged_unique() {
            PackTemplate singleSlot = new PackTemplate();
            singleSlot.setId(21L);
            singleSlot.setName("Single Unique");

            PackSlot slot = new PackSlot();
            slot.setId(21L);
            slot.setPackTemplate(singleSlot);
            slot.setFixedRarity(Rarity.RARE);
            singleSlot.setSlots(List.of(slot));

            Card uniqueCard = new Card();
            uniqueCard.setId(51L);
            uniqueCard.setName("rare_unique");
            uniqueCard.setRarity(Rarity.RARE);
            uniqueCard.setDropRate(0.5);
            uniqueCard.setUniqueCard(true);
            uniqueCard.setCreatedBy(alice);

            when(repositoryPackTemplate.findById(singleSlot.getId())).thenReturn(Optional.of(singleSlot));
            when(repositoryCard.findByRarityAndUserIsNull(Rarity.RARE)).thenReturn(List.of(uniqueCard));
            when(repositoryCard.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
            when(repositoryCard.findAllById(anyList())).thenReturn(List.of(uniqueCard));
            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));

            servicePack.generatePack(alice.getId(), singleSlot.getId());

            assertEquals(0.5, uniqueCard.getDropRate(), 0.001);
        }

        @Test
        @DisplayName("dropRate never goes below floor (0.01)")
        void testDropRateFloor() {
            PackTemplate singleSlot = new PackTemplate();
            singleSlot.setId(22L);
            singleSlot.setName("Floor Test");

            PackSlot slot = new PackSlot();
            slot.setId(22L);
            slot.setPackTemplate(singleSlot);
            slot.setFixedRarity(Rarity.COMMON);
            singleSlot.setSlots(List.of(slot));

            Card almostZero = new Card();
            almostZero.setId(52L);
            almostZero.setName("almost_zero");
            almostZero.setRarity(Rarity.COMMON);
            almostZero.setDropRate(0.005); // below floor after reduction
            almostZero.setUniqueCard(false);
            almostZero.setCreatedBy(alice);

            when(repositoryPackTemplate.findById(singleSlot.getId())).thenReturn(Optional.of(singleSlot));
            when(repositoryCard.findByRarityAndUserIsNull(Rarity.COMMON)).thenReturn(List.of(almostZero));
            when(repositoryCard.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
            when(repositoryCard.findAllById(anyList())).thenReturn(List.of(almostZero));
            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));

            servicePack.generatePack(alice.getId(), singleSlot.getId());

            // Max(0.01, 0.005 * 0.95) = Max(0.01, 0.00475) = 0.01
            assertEquals(0.01, almostZero.getDropRate(), 0.001);
        }
    }


    // ========================================
    //         SLOT MECHANICS TESTS
    // ========================================

    @Nested
    @DisplayName("Slot mechanics")
    class SlotTests {

        @Test
        @DisplayName("fixed rarity slot always picks from that rarity pool")
        void testFixedRaritySlot() {
            PackTemplate fixedTemplate = new PackTemplate();
            fixedTemplate.setId(30L);
            fixedTemplate.setName("Fixed Epic");

            PackSlot slot = new PackSlot();
            slot.setId(30L);
            slot.setPackTemplate(fixedTemplate);
            slot.setFixedRarity(Rarity.EPIC);
            fixedTemplate.setSlots(List.of(slot));

            List<Card> epicPool = cards.stream().filter(c -> c.getRarity() == Rarity.EPIC).toList();

            when(repositoryPackTemplate.findById(fixedTemplate.getId())).thenReturn(Optional.of(fixedTemplate));
            when(repositoryCard.findByRarityAndUserIsNull(Rarity.EPIC)).thenReturn(epicPool);
            when(repositoryCard.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
            when(repositoryCard.findAllById(anyList())).thenAnswer(inv -> {
                List<Long> ids = inv.getArgument(0);
                return cards.stream().filter(c -> ids.contains(c.getId())).toList();
            });
            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));

            List<CardDto> result = servicePack.generatePack(alice.getId(), fixedTemplate.getId());

            assertEquals(1, result.size());
            assertEquals(Rarity.EPIC, result.getFirst().rarity());
        }

        @Test
        @DisplayName("slot with no fixedRarity and no rarityWeights throws")
        void testSlot_noRarityConfig() {
            PackTemplate badTemplate = new PackTemplate();
            badTemplate.setId(31L);
            badTemplate.setName("Bad Slot");

            PackSlot emptySlot = new PackSlot();
            emptySlot.setId(31L);
            emptySlot.setPackTemplate(badTemplate);
            // No fixedRarity, no rarityWeights
            emptySlot.setRarityWeights(new HashMap<>());
            badTemplate.setSlots(List.of(emptySlot));

            when(repositoryPackTemplate.findById(badTemplate.getId())).thenReturn(Optional.of(badTemplate));

            assertThrows(IllegalStateException.class,
                    () -> servicePack.generatePack(alice.getId(), badTemplate.getId()));
        }

        @Test
        @DisplayName("cards already picked in same pack are excluded from subsequent slots")
        void testNoDuplicatesWithinPack() {
            // 2-slot template, both fixed LENGENDARY — only 2 legendary cards exist
            PackTemplate twoSlot = new PackTemplate();
            twoSlot.setId(32L);
            twoSlot.setName("Two Legendary");

            PackSlot slot1 = new PackSlot();
            slot1.setId(32L);
            slot1.setPackTemplate(twoSlot);
            slot1.setFixedRarity(Rarity.LEGENDARY);

            PackSlot slot2 = new PackSlot();
            slot2.setId(33L);
            slot2.setPackTemplate(twoSlot);
            slot2.setFixedRarity(Rarity.LEGENDARY);
            twoSlot.setSlots(List.of(slot1, slot2));

            List<Card> legendaryPool = cards.stream().filter(c -> c.getRarity() == Rarity.LEGENDARY).toList();
            Card leg1 = legendaryPool.get(0);
            Card leg2 = legendaryPool.get(1);

            // First slot: full pool
            when(repositoryCard.findByRarityAndUserIsNull(Rarity.LEGENDARY)).thenReturn(List.of(leg1, leg2));
            // Second slot: exclude picked card — return only the other
            when(repositoryCard.findByRarityAndUserIsNullAndIdNotIn(eq(Rarity.LEGENDARY), anyList()))
                    .thenAnswer(inv -> {
                        List<Long> excluded = inv.getArgument(1);
                        return legendaryPool.stream().filter(c -> !excluded.contains(c.getId())).toList();
                    });

            when(repositoryPackTemplate.findById(twoSlot.getId())).thenReturn(Optional.of(twoSlot));
            when(repositoryCard.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
            when(repositoryCard.findAllById(anyList())).thenAnswer(inv -> {
                List<Long> ids = inv.getArgument(0);
                return legendaryPool.stream().filter(c -> ids.contains(c.getId())).toList();
            });
            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));

            List<CardDto> result = servicePack.generatePack(alice.getId(), twoSlot.getId());

            assertEquals(2, result.size());
            // The two cards should be different
            assertNotEquals(result.get(0).id(), result.get(1).id());
        }
    }
}
