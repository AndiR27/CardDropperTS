package ts.backend_carddropper.TestServices;

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
import ts.backend_carddropper.entity.PackTemplateSlot;
import ts.backend_carddropper.entity.User;
import ts.backend_carddropper.entity.UserCard;
import ts.backend_carddropper.entity.UserPackInventory;
import ts.backend_carddropper.enums.Rarity;
import ts.backend_carddropper.helper.TestDataHelper;
import ts.backend_carddropper.models.CardDto;
import ts.backend_carddropper.repository.RepositoryCard;
import ts.backend_carddropper.repository.RepositoryPackSlot;
import ts.backend_carddropper.repository.RepositoryPackTemplate;
import ts.backend_carddropper.repository.RepositoryUser;
import ts.backend_carddropper.repository.RepositoryUserCard;
import ts.backend_carddropper.repository.RepositoryUserPackInventory;
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

    @MockitoBean
    private RepositoryPackSlot repositoryPackSlot;

    @MockitoBean
    private RepositoryUserPackInventory repositoryUserPackInventory;

    @MockitoBean
    private RepositoryUserCard repositoryUserCard;

    @MockitoBean
    private ts.backend_carddropper.repository.RepositoryLiveFeed repositoryLiveFeed;

    private User alice;
    private User bob;
    private List<Card> cards;
    private PackTemplate packTemplate;

    @BeforeEach
    void setUp() {
        List<User> users = testDataHelper.createUsers();
        alice = users.getFirst();
        bob = users.get(1);
        cards = testDataHelper.createCards(alice);
        packTemplate = testDataHelper.createPackTemplate();

        // Default: repositoryLiveFeed.save returns a saved entity (needed for LegendaryDropEvent)
        when(repositoryLiveFeed.save(any(ts.backend_carddropper.entity.LiveFeedEvent.class)))
                .thenAnswer(inv -> {
                    ts.backend_carddropper.entity.LiveFeedEvent e = inv.getArgument(0);
                    e.setId(1L);
                    e.setCreatedAt(java.time.LocalDateTime.now());
                    return e;
                });
    }

    private void mockAllPools() {
        for (Rarity rarity : Rarity.values()) {
            List<Card> pool = cards.stream().filter(c -> c.getRarity() == rarity).toList();
            when(repositoryCard.findPoolCardsByRarity(eq(rarity), anyLong())).thenReturn(pool);
            when(repositoryCard.findPoolCardsByRarityExcluding(eq(rarity), anyLong(), anyList()))
                    .thenAnswer(inv -> {
                        List<Long> excluded = inv.getArgument(2);
                        return pool.stream().filter(c -> !excluded.contains(c.getId())).toList();
                    });
        }
    }

    private void mockPackInfrastructure() {
        when(repositoryCard.findAllById(anyList())).thenAnswer(inv -> {
            List<Long> ids = inv.getArgument(0);
            return cards.stream().filter(c -> ids.contains(c.getId())).toList();
        });
        when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
        // alice owns no cards by default; addCardToUser will save new UserCards
        when(repositoryUserCard.findByUserId(alice.getId())).thenReturn(List.of());
        when(repositoryUserCard.findByUserIdAndCardId(eq(alice.getId()), anyLong()))
                .thenReturn(Optional.empty());
        when(repositoryUserCard.save(any(UserCard.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    /**
     * Mocks the inventory so the user owns packs of the given template.
     */
    private void mockInventory(User user, PackTemplate template, int quantity) {
        UserPackInventory inv = new UserPackInventory();
        inv.setId(1L);
        inv.setUser(user);
        inv.setPackTemplate(template);
        inv.setQuantity(quantity);
        when(repositoryUserPackInventory.findByUserIdAndPackTemplateId(user.getId(), template.getId()))
                .thenReturn(Optional.of(inv));
    }

    /**
     * Helper to create a single-slot template using PackTemplateSlot.
     */
    private PackTemplate createSingleSlotTemplate(Long templateId, String name, PackSlot slot, int count) {
        PackTemplate template = new PackTemplate();
        template.setId(templateId);
        template.setName(name);

        PackTemplateSlot ts = new PackTemplateSlot();
        ts.setId(templateId);
        ts.setPackTemplate(template);
        ts.setPackSlot(slot);
        ts.setCount(count);
        template.setSlots(List.of(ts));

        return template;
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
            mockInventory(alice, packTemplate, 1);
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
            mockInventory(alice, packTemplate, 1);
            when(repositoryPackTemplate.findById(packTemplate.getId())).thenReturn(Optional.of(packTemplate));
            mockAllPools();
            mockPackInfrastructure();

            List<CardDto> result = servicePack.generatePack(alice.getId(), packTemplate.getId());

            // 3 cards should have been saved as new UserCard entries
            verify(repositoryUserCard, times(3)).save(any(UserCard.class));
        }

        @Test
        @DisplayName("throws when user has no packs of this template")
        void testGeneratePack_noPacks() {
            when(repositoryUserPackInventory.findByUserIdAndPackTemplateId(alice.getId(), 999L))
                    .thenReturn(Optional.empty());

            assertThrows(IllegalStateException.class,
                    () -> servicePack.generatePack(alice.getId(), 999L));
        }

        @Test
        @DisplayName("throws when pool is empty for a required rarity")
        void testGeneratePack_emptyPool() {
            PackSlot legendarySlot = new PackSlot();
            legendarySlot.setId(10L);
            legendarySlot.setName("fixedLegendary");
            legendarySlot.setFixedRarity(Rarity.LEGENDARY);

            PackTemplate legendaryTemplate = createSingleSlotTemplate(10L, "Legendary Only", legendarySlot, 1);

            mockInventory(alice, legendaryTemplate, 1);
            when(repositoryPackTemplate.findById(legendaryTemplate.getId())).thenReturn(Optional.of(legendaryTemplate));
            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryCard.findPoolCardsByRarity(eq(Rarity.LEGENDARY), anyLong())).thenReturn(Collections.emptyList());

            assertThrows(IllegalStateException.class,
                    () -> servicePack.generatePack(alice.getId(), legendaryTemplate.getId()));
        }
    }


    // ========================================
    //     OWNER-BASED WEIGHTING TESTS
    // ========================================

    @Nested
    @DisplayName("Owner-based weighting")
    class WeightingTests {

        @Test
        @DisplayName("cards with fewer owners are more likely to be picked")
        void testOwnerWeighting_favorsLessOwned() {
            Card lessOwned = new Card();
            lessOwned.setId(100L);
            lessOwned.setName("less_owned");
            lessOwned.setRarity(Rarity.COMMON);
            lessOwned.setDropRate(1.0);
            lessOwned.setUniqueCard(false);
            lessOwned.setCreatedBy(alice);

            Card moreOwned = new Card();
            moreOwned.setId(101L);
            moreOwned.setName("more_owned");
            moreOwned.setRarity(Rarity.COMMON);
            moreOwned.setDropRate(1.0);
            moreOwned.setUniqueCard(false);
            moreOwned.setCreatedBy(alice);
            for (int i = 0; i < 10; i++) {
                User owner = new User();
                owner.setId(100L + i);
                owner.setUsername("user" + i);
                moreOwned.getUserCards().add(new UserCard(owner, moreOwned, 1));
            }

            PackSlot commonSlot = new PackSlot();
            commonSlot.setId(30L);
            commonSlot.setName("fixedCommon");
            commonSlot.setFixedRarity(Rarity.COMMON);

            PackTemplate singleSlot = createSingleSlotTemplate(30L, "Weighting Test", commonSlot, 1);

            mockInventory(alice, singleSlot, 10000);
            when(repositoryPackTemplate.findById(singleSlot.getId())).thenReturn(Optional.of(singleSlot));
            when(repositoryCard.findPoolCardsByRarity(eq(Rarity.COMMON), anyLong())).thenReturn(List.of(lessOwned, moreOwned));
            when(repositoryCard.findAllById(anyList())).thenAnswer(inv -> {
                List<Long> ids = inv.getArgument(0);
                return List.of(lessOwned, moreOwned).stream()
                        .filter(c -> ids.contains(c.getId())).toList();
            });
            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));

            int lessOwnedCount = 0;
            int iterations = 1000;
            for (int i = 0; i < iterations; i++) {
                // reset mocks for next iteration (alice owns nothing yet)
                reset(repositoryUserCard);
                when(repositoryUserCard.findByUserId(alice.getId())).thenReturn(List.of());
                when(repositoryUserCard.findByUserIdAndCardId(eq(alice.getId()), anyLong())).thenReturn(Optional.empty());
                when(repositoryUserCard.save(any(UserCard.class))).thenAnswer(inv -> inv.getArgument(0));
                List<CardDto> result = servicePack.generatePack(alice.getId(), singleSlot.getId());
                if (result.getFirst().id().equals(lessOwned.getId())) {
                    lessOwnedCount++;
                }
            }

            double lessOwnedRatio = (double) lessOwnedCount / iterations;
            assertTrue(lessOwnedRatio > 0.80,
                    "Card with 0 owners should be picked >80% of the time, was " + lessOwnedRatio);
            assertTrue(lessOwnedRatio < 0.99,
                    "Card with 10 owners should still be picked sometimes, ratio was " + lessOwnedRatio);
        }

        @Test
        @DisplayName("cards with equal owner count have equal probability")
        void testOwnerWeighting_equalOwners() {
            Card card1 = cards.get(0);
            Card card2 = cards.get(1);

            PackSlot commonSlot = new PackSlot();
            commonSlot.setId(31L);
            commonSlot.setName("fixedCommonEqual");
            commonSlot.setFixedRarity(Rarity.COMMON);

            PackTemplate singleSlot = createSingleSlotTemplate(31L, "Equal Test", commonSlot, 1);

            mockInventory(alice, singleSlot, 10000);
            when(repositoryPackTemplate.findById(singleSlot.getId())).thenReturn(Optional.of(singleSlot));
            when(repositoryCard.findPoolCardsByRarity(eq(Rarity.COMMON), anyLong())).thenReturn(List.of(card1, card2));
            when(repositoryCard.findAllById(anyList())).thenAnswer(inv -> {
                List<Long> ids = inv.getArgument(0);
                return List.of(card1, card2).stream()
                        .filter(c -> ids.contains(c.getId())).toList();
            });
            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));

            int card1Count = 0;
            int iterations = 1000;
            for (int i = 0; i < iterations; i++) {
                // reset mocks for next iteration (alice owns nothing yet)
                reset(repositoryUserCard);
                when(repositoryUserCard.findByUserId(alice.getId())).thenReturn(List.of());
                when(repositoryUserCard.findByUserIdAndCardId(eq(alice.getId()), anyLong())).thenReturn(Optional.empty());
                when(repositoryUserCard.save(any(UserCard.class))).thenAnswer(inv -> inv.getArgument(0));
                List<CardDto> result = servicePack.generatePack(alice.getId(), singleSlot.getId());
                if (result.getFirst().id().equals(card1.getId())) {
                    card1Count++;
                }
            }

            double ratio = (double) card1Count / iterations;
            assertTrue(ratio > 0.35 && ratio < 0.65,
                    "Two cards with equal owners should be picked roughly equally, ratio was " + ratio);
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
            PackSlot epicSlot = new PackSlot();
            epicSlot.setId(30L);
            epicSlot.setName("fixedEpic");
            epicSlot.setFixedRarity(Rarity.EPIC);

            PackTemplate fixedTemplate = createSingleSlotTemplate(30L, "Fixed Epic", epicSlot, 1);

            List<Card> epicPool = cards.stream().filter(c -> c.getRarity() == Rarity.EPIC).toList();

            mockInventory(alice, fixedTemplate, 1);
            when(repositoryPackTemplate.findById(fixedTemplate.getId())).thenReturn(Optional.of(fixedTemplate));
            when(repositoryCard.findPoolCardsByRarity(eq(Rarity.EPIC), anyLong())).thenReturn(epicPool);
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
            PackSlot emptySlot = new PackSlot();
            emptySlot.setId(31L);
            emptySlot.setName("emptySlot");
            emptySlot.setRarityWeights(new HashMap<>());

            PackTemplate badTemplate = createSingleSlotTemplate(31L, "Bad Slot", emptySlot, 1);

            mockInventory(alice, badTemplate, 1);
            when(repositoryPackTemplate.findById(badTemplate.getId())).thenReturn(Optional.of(badTemplate));
            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));

            assertThrows(IllegalStateException.class,
                    () -> servicePack.generatePack(alice.getId(), badTemplate.getId()));
        }

        @Test
        @DisplayName("cards already picked in same pack are excluded from subsequent slots")
        void testNoDuplicatesWithinPack() {
            PackSlot legendarySlot = new PackSlot();
            legendarySlot.setId(32L);
            legendarySlot.setName("fixedLegendaryDup");
            legendarySlot.setFixedRarity(Rarity.LEGENDARY);

            // Use count=2 to get 2 cards from the same slot
            PackTemplate twoSlot = createSingleSlotTemplate(32L, "Two Legendary", legendarySlot, 2);

            List<Card> legendaryPool = cards.stream().filter(c -> c.getRarity() == Rarity.LEGENDARY).toList();

            when(repositoryCard.findPoolCardsByRarity(eq(Rarity.LEGENDARY), anyLong())).thenReturn(legendaryPool);
            when(repositoryCard.findPoolCardsByRarityExcluding(eq(Rarity.LEGENDARY), anyLong(), anyList()))
                    .thenAnswer(inv -> {
                        List<Long> excluded = inv.getArgument(2);
                        return legendaryPool.stream().filter(c -> !excluded.contains(c.getId())).toList();
                    });

            mockInventory(alice, twoSlot, 1);
            when(repositoryPackTemplate.findById(twoSlot.getId())).thenReturn(Optional.of(twoSlot));
            when(repositoryCard.findAllById(anyList())).thenAnswer(inv -> {
                List<Long> ids = inv.getArgument(0);
                return legendaryPool.stream().filter(c -> ids.contains(c.getId())).toList();
            });
            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));

            List<CardDto> result = servicePack.generatePack(alice.getId(), twoSlot.getId());

            assertEquals(2, result.size());
            assertNotEquals(result.get(0).id(), result.get(1).id());
        }
    }


    // ========================================
    //         TARGETED CARD TESTS
    // ========================================

    @Nested
    @DisplayName("Targeted card exclusion from drop pool")
    class TargetedCardTests {

        private PackTemplate commonTemplate;
        private PackSlot commonSlot;

        @BeforeEach
        void setUpTargetedTests() {
            commonSlot = new PackSlot();
            commonSlot.setId(40L);
            commonSlot.setName("fixedCommonTarget");
            commonSlot.setFixedRarity(Rarity.COMMON);
            commonTemplate = createSingleSlotTemplate(40L, "Targeted Test", commonSlot, 1);
        }

        @Test
        @DisplayName("targeted card never appears in its target user's pack")
        void testTargetedCard_excludedFromTargetUserPool() {
            Card normalCard = new Card();
            normalCard.setId(200L);
            normalCard.setName("normal_card");
            normalCard.setRarity(Rarity.COMMON);
            normalCard.setDropRate(1.0);
            normalCard.setUniqueCard(false);

            Card targetedCard = new Card();
            targetedCard.setId(201L);
            targetedCard.setName("targeted_card");
            targetedCard.setRarity(Rarity.COMMON);
            targetedCard.setDropRate(1.0);
            targetedCard.setUniqueCard(false);
            targetedCard.setTargetUser(alice); // targeted at alice

            // Pool query for alice excludes the card targeting alice
            when(repositoryCard.findPoolCardsByRarity(eq(Rarity.COMMON), eq(alice.getId())))
                    .thenReturn(List.of(normalCard)); // only normalCard returned

            mockInventory(alice, commonTemplate, 1);
            when(repositoryPackTemplate.findById(commonTemplate.getId())).thenReturn(Optional.of(commonTemplate));
            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryUserCard.findByUserId(alice.getId())).thenReturn(List.of());
            when(repositoryUserCard.findByUserIdAndCardId(eq(alice.getId()), anyLong())).thenReturn(Optional.empty());
            when(repositoryUserCard.save(any(UserCard.class))).thenAnswer(inv -> inv.getArgument(0));
            when(repositoryCard.findAllById(anyList())).thenAnswer(inv -> {
                List<Long> ids = inv.getArgument(0);
                return List.of(normalCard, targetedCard).stream().filter(c -> ids.contains(c.getId())).toList();
            });

            List<CardDto> result = servicePack.generatePack(alice.getId(), commonTemplate.getId());

            assertEquals(1, result.size());
            assertEquals(normalCard.getId(), result.getFirst().id(),
                    "Targeted card must not appear in alice's pack — she is the target");
        }

        @Test
        @DisplayName("targeted card can appear in other users' packs")
        void testTargetedCard_allowedForOtherUsers() {
            Card targetedCard = new Card();
            targetedCard.setId(202L);
            targetedCard.setName("targeted_for_alice");
            targetedCard.setRarity(Rarity.COMMON);
            targetedCard.setDropRate(1.0);
            targetedCard.setUniqueCard(false);
            targetedCard.setTargetUser(alice); // targeted at alice, NOT bob

            // Pool query for bob includes the targeted card (he is not the target)
            when(repositoryCard.findPoolCardsByRarity(eq(Rarity.COMMON), eq(bob.getId())))
                    .thenReturn(List.of(targetedCard));

            mockInventory(bob, commonTemplate, 1);
            when(repositoryUserPackInventory.findByUserIdAndPackTemplateId(bob.getId(), commonTemplate.getId()))
                    .thenAnswer(inv -> {
                        var inventory = new UserPackInventory();
                        inventory.setId(2L);
                        inventory.setUser(bob);
                        inventory.setPackTemplate(commonTemplate);
                        inventory.setQuantity(1);
                        return Optional.of(inventory);
                    });
            when(repositoryPackTemplate.findById(commonTemplate.getId())).thenReturn(Optional.of(commonTemplate));
            when(repositoryUser.findById(bob.getId())).thenReturn(Optional.of(bob));
            when(repositoryUserCard.findByUserId(bob.getId())).thenReturn(List.of());
            when(repositoryUserCard.findByUserIdAndCardId(eq(bob.getId()), anyLong())).thenReturn(Optional.empty());
            when(repositoryUserCard.save(any(UserCard.class))).thenAnswer(inv -> inv.getArgument(0));
            when(repositoryCard.findAllById(anyList())).thenAnswer(inv -> {
                List<Long> ids = inv.getArgument(0);
                return List.of(targetedCard).stream().filter(c -> ids.contains(c.getId())).toList();
            });

            List<CardDto> result = servicePack.generatePack(bob.getId(), commonTemplate.getId());

            assertEquals(1, result.size());
            assertEquals(targetedCard.getId(), result.getFirst().id(),
                    "Targeted card should appear in bob's pack — he is not the target");
        }

        @Test
        @DisplayName("throws when only card in pool targets the opener")
        void testTargetedCard_emptyPoolThrows() {
            // Pool is empty because the only card targets alice and alice is opening
            when(repositoryCard.findPoolCardsByRarity(eq(Rarity.COMMON), eq(alice.getId())))
                    .thenReturn(List.of());

            mockInventory(alice, commonTemplate, 1);
            when(repositoryPackTemplate.findById(commonTemplate.getId())).thenReturn(Optional.of(commonTemplate));
            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryUserCard.findByUserId(alice.getId())).thenReturn(List.of());

            assertThrows(IllegalStateException.class,
                    () -> servicePack.generatePack(alice.getId(), commonTemplate.getId()));
        }
    }
}
