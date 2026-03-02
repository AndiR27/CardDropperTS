package ts.backend_carddropper.TestServices;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ts.backend_carddropper.entity.Card;
import ts.backend_carddropper.entity.User;
import ts.backend_carddropper.enums.Rarity;
import ts.backend_carddropper.helper.TestDataHelper;
import ts.backend_carddropper.models.CardDto;
import ts.backend_carddropper.repository.RepositoryCard;
import ts.backend_carddropper.repository.RepositoryUser;
import ts.backend_carddropper.service.ServiceCard;
import ts.backend_carddropper.utils.ImageUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class TestServiceCard {

    @Autowired
    private ServiceCard serviceCard;

    @Autowired
    private TestDataHelper testDataHelper;

    @Value("${app.card-images.dir}")
    private String cardImagesDir;

    @MockitoBean
    private RepositoryCard repositoryCard;

    @MockitoBean
    private RepositoryUser repositoryUser;

    private List<User> users;
    private List<Card> cards;
    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        users = testDataHelper.createUsers();
        alice = users.getFirst();
        bob = users.get(1);
        cards = testDataHelper.createCards(alice);
    }


    // ========================================
    //              FIND TESTS
    // ========================================

    @Nested
    @DisplayName("Find operations")
    class FindTests {

        @Test
        @DisplayName("findById returns card when exists")
        void testFindById_exists() {
            Card card = cards.getFirst();
            when(repositoryCard.findById(card.getId())).thenReturn(Optional.of(card));

            Optional<CardDto> result = serviceCard.findById(card.getId());

            assertTrue(result.isPresent());
            assertEquals(card.getName(), result.get().name());
            assertEquals(card.getRarity(), result.get().rarity());
        }

        @Test
        @DisplayName("findById returns empty when card does not exist")
        void testFindById_notFound() {
            when(repositoryCard.findById(999L)).thenReturn(Optional.empty());

            Optional<CardDto> result = serviceCard.findById(999L);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("findAll returns all cards")
        void testFindAll() {
            when(repositoryCard.findAll()).thenReturn(cards);

            List<CardDto> result = serviceCard.findAll();

            assertEquals(24, result.size());
        }

        @Test
        @DisplayName("findByRarity returns only cards of that rarity")
        void testFindByRarity() {
            List<Card> rareCards = cards.stream().filter(c -> c.getRarity() == Rarity.RARE).toList();
            when(repositoryCard.findByRarity(Rarity.RARE)).thenReturn(rareCards);

            List<CardDto> result = serviceCard.findByRarity(Rarity.RARE);

            assertEquals(6, result.size());
            assertTrue(result.stream().allMatch(c -> c.rarity() == Rarity.RARE));
        }

        @Test
        @DisplayName("findAllByUserId returns cards owned by user")
        void testFindAllByUserId() {
            Card ownedCard = cards.getFirst();
            ownedCard.setUser(alice);

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryCard.findAllByUserId(alice.getId())).thenReturn(List.of(ownedCard));

            List<CardDto> result = serviceCard.findAllByUserId(alice.getId());

            assertEquals(1, result.size());
            assertEquals(ownedCard.getName(), result.getFirst().name());
        }

        @Test
        @DisplayName("findAllByUserId throws when user not found")
        void testFindAllByUserId_userNotFound() {
            when(repositoryUser.findById(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> serviceCard.findAllByUserId(999L));
        }

        @Test
        @DisplayName("findAllByCreatedById returns cards created by user")
        void testFindAllByCreatedById() {
            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryCard.findAllByCreatedById(alice.getId())).thenReturn(cards);

            List<CardDto> result = serviceCard.findAllByCreatedById(alice.getId());

            assertEquals(24, result.size());
        }

        @Test
        @DisplayName("findAllByTargetUserId returns cards targeting user")
        void testFindAllByTargetUserId() {
            Card targeted = cards.getFirst();
            targeted.setTargetUser(alice);

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryCard.findAllByTargetUserId(alice.getId())).thenReturn(List.of(targeted));

            List<CardDto> result = serviceCard.findAllByTargetUserId(alice.getId());

            assertEquals(1, result.size());
            assertEquals(alice.getId(), result.getFirst().targetUserId());
        }
    }


    // ========================================
    //         CREATE / UPDATE / DELETE
    // ========================================

    @Nested
    @DisplayName("Create, Update, Delete operations")
    class CrudTests {

        @Test
        @DisplayName("createCard with owner, creator and no target")
        void testCreateCard_withOwnerAndCreator() {
            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryUser.findById(bob.getId())).thenReturn(Optional.of(bob));

            Card savedCard = new Card();
            savedCard.setId(100L);
            savedCard.setName("TestCard");
            savedCard.setRarity(Rarity.EPIC);
            savedCard.setDropRate(0.2);
            savedCard.setUser(alice);
            savedCard.setCreatedBy(bob);

            when(repositoryCard.save(any(Card.class))).thenReturn(savedCard);

            CardDto dto = new CardDto(null, "TestCard", null, Rarity.EPIC, null, 0.2, false, alice.getId(), bob.getId(), null);
            CardDto result = serviceCard.createCard(dto);

            assertEquals("TestCard", result.name());
            assertEquals(Rarity.EPIC, result.rarity());
            assertEquals(alice.getId(), result.userId());
            assertEquals(bob.getId(), result.createdById());
            assertNull(result.targetUserId());
        }

        @Test
        @DisplayName("createCard with targetUser equal to creator succeeds")
        void testCreateCard_targetSelf() {
            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));

            Card savedCard = new Card();
            savedCard.setId(101L);
            savedCard.setName("SelfTarget");
            savedCard.setRarity(Rarity.COMMON);
            savedCard.setDropRate(1.0);
            savedCard.setCreatedBy(alice);
            savedCard.setTargetUser(alice);

            when(repositoryCard.save(any(Card.class))).thenReturn(savedCard);

            CardDto dto = new CardDto(null, "SelfTarget", null, Rarity.COMMON, null, 1.0, false, null, alice.getId(), alice.getId());
            CardDto result = serviceCard.createCard(dto);

            assertEquals(alice.getId(), result.createdById());
            assertEquals(alice.getId(), result.targetUserId());
        }

        @Test
        @DisplayName("createCard fails when targetUser differs from creator")
        void testCreateCard_targetNotCreator() {
            CardDto dto = new CardDto(null, "Bad", null, Rarity.COMMON, null, 1.0, false, null, alice.getId(), bob.getId());

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> serviceCard.createCard(dto)
            );
            assertTrue(ex.getMessage().contains("targetUser must be the card creator itself"));
        }

        @Test
        @DisplayName("update card successfully")
        void testUpdate_success() {
            Card card = cards.getFirst();
            when(repositoryCard.findById(card.getId())).thenReturn(Optional.of(card));

            Card updatedCard = new Card();
            updatedCard.setId(card.getId());
            updatedCard.setName("UpdatedName");
            updatedCard.setRarity(Rarity.COMMON);
            updatedCard.setDropRate(0.8);
            updatedCard.setCreatedBy(alice);

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryCard.save(any(Card.class))).thenReturn(updatedCard);

            CardDto dto = new CardDto(card.getId(), "UpdatedName", null, Rarity.COMMON, null, 0.8, false, null, alice.getId(), null);
            Optional<CardDto> result = serviceCard.update(card.getId(), dto);

            assertTrue(result.isPresent());
            assertEquals("UpdatedName", result.get().name());
            verify(repositoryCard).save(any(Card.class));
        }

        @Test
        @DisplayName("update returns empty when card not found")
        void testUpdate_notFound() {
            when(repositoryCard.findById(999L)).thenReturn(Optional.empty());

            CardDto dto = new CardDto(999L, "Ghost", null, Rarity.COMMON, null, 1.0, false, null, null, null);
            Optional<CardDto> result = serviceCard.update(999L, dto);

            assertTrue(result.isEmpty());
            verify(repositoryCard, never()).save(any());
        }

        @Test
        @DisplayName("delete card successfully")
        void testDelete_success() {
            when(repositoryCard.existsById(1L)).thenReturn(true);

            serviceCard.delete(1L);

            verify(repositoryCard).deleteById(1L);
        }

        @Test
        @DisplayName("delete throws when card not found")
        void testDelete_notFound() {
            when(repositoryCard.existsById(999L)).thenReturn(false);

            assertThrows(EntityNotFoundException.class, () -> serviceCard.delete(999L));
            verify(repositoryCard, never()).deleteById(any());
        }
    }


    // ========================================
    //      TRANSFER OWNERSHIP TESTS
    // ========================================

    @Nested
    @DisplayName("Transfer ownership")
    class TransferTests {

        @Test
        @DisplayName("transferOwnership assigns card to new user")
        void testTransferOwnership_success() {
            Card card = cards.getFirst();
            card.setUser(alice);

            when(repositoryCard.findById(card.getId())).thenReturn(Optional.of(card));
            when(repositoryUser.findById(bob.getId())).thenReturn(Optional.of(bob));
            when(repositoryCard.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

            CardDto result = serviceCard.transferOwnership(card.getId(), bob.getId());

            assertEquals(bob.getId(), result.userId());
            assertEquals(bob, card.getUser());
        }

        @Test
        @DisplayName("transferOwnership throws when card not found")
        void testTransferOwnership_cardNotFound() {
            when(repositoryCard.findById(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> serviceCard.transferOwnership(999L, bob.getId()));
        }

        @Test
        @DisplayName("transferOwnership throws when new owner not found")
        void testTransferOwnership_userNotFound() {
            Card card = cards.getFirst();
            when(repositoryCard.findById(card.getId())).thenReturn(Optional.of(card));
            when(repositoryUser.findById(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> serviceCard.transferOwnership(card.getId(), 999L));
        }
    }


    // ========================================
    //      USER-CARD RELATION TESTS
    // ========================================

    @Nested
    @DisplayName("Card-User relations integrity")
    class RelationTests {

        @Test
        @DisplayName("card with all 3 user relations (owner, creator, target) maps correctly")
        void testAllRelations() {
            Card card = cards.getFirst();
            card.setUser(alice);
            card.setCreatedBy(bob);
            card.setTargetUser(alice);

            when(repositoryCard.findById(card.getId())).thenReturn(Optional.of(card));

            Optional<CardDto> result = serviceCard.findById(card.getId());

            assertTrue(result.isPresent());
            CardDto dto = result.get();
            assertEquals(alice.getId(), dto.userId());
            assertEquals(bob.getId(), dto.createdById());
            assertEquals(alice.getId(), dto.targetUserId());
        }

        @Test
        @DisplayName("pool card (no owner) has null userId in DTO")
        void testPoolCardRelation() {
            Card poolCard = cards.getFirst();
            // user is already null (pool card)
            assertNull(poolCard.getUser());

            when(repositoryCard.findById(poolCard.getId())).thenReturn(Optional.of(poolCard));

            Optional<CardDto> result = serviceCard.findById(poolCard.getId());

            assertTrue(result.isPresent());
            assertNull(result.get().userId());
            assertEquals(alice.getId(), result.get().createdById());
        }

        @Test
        @DisplayName("createCard applies all user relations via applyUserRelations")
        void testCreateCard_appliesAllRelations() {
            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryUser.findById(bob.getId())).thenReturn(Optional.of(bob));

            Card savedCard = new Card();
            savedCard.setId(200L);
            savedCard.setName("FullRelation");
            savedCard.setRarity(Rarity.RARE);
            savedCard.setDropRate(0.5);
            savedCard.setUser(bob);
            savedCard.setCreatedBy(alice);
            savedCard.setTargetUser(alice);

            when(repositoryCard.save(any(Card.class))).thenReturn(savedCard);

            // creator=alice, owner=bob, target=alice (target == creator so allowed)
            CardDto dto = new CardDto(null, "FullRelation", null, Rarity.RARE, null, 0.5, false, bob.getId(), alice.getId(), alice.getId());
            CardDto result = serviceCard.createCard(dto);

            assertEquals(bob.getId(), result.userId());
            assertEquals(alice.getId(), result.createdById());
            assertEquals(alice.getId(), result.targetUserId());
        }

        @Test
        @DisplayName("update card reassigns user relations")
        void testUpdate_reassignsRelations() {
            Card card = cards.getFirst();
            card.setUser(alice);

            when(repositoryCard.findById(card.getId())).thenReturn(Optional.of(card));
            when(repositoryUser.findById(bob.getId())).thenReturn(Optional.of(bob));

            Card updatedCard = new Card();
            updatedCard.setId(card.getId());
            updatedCard.setName(card.getName());
            updatedCard.setRarity(card.getRarity());
            updatedCard.setDropRate(card.getDropRate());
            updatedCard.setUser(bob);
            updatedCard.setCreatedBy(alice);

            when(repositoryCard.save(any(Card.class))).thenReturn(updatedCard);

            // Transfer ownership from alice to bob via update
            CardDto dto = new CardDto(card.getId(), card.getName(), null, card.getRarity(), null, card.getDropRate(), false, bob.getId(), null, null);
            Optional<CardDto> result = serviceCard.update(card.getId(), dto);

            assertTrue(result.isPresent());
            assertEquals(bob.getId(), result.get().userId());
        }
    }


    // ========================================
    //      IMAGE UPLOAD TESTS
    // ========================================

    @Nested
    @DisplayName("Image upload operations")
    class ImageUploadTests {

        @Test
        @DisplayName("createCardWithImage stores image and sets imageUrl")
        void testCreateCardWithImage_valid() {
            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));

            Card savedCard = new Card();
            savedCard.setId(50L);
            savedCard.setName("Dragon");
            savedCard.setRarity(Rarity.EPIC);
            savedCard.setDropRate(0.2);
            savedCard.setCreatedBy(alice);

            Card updatedCard = new Card();
            updatedCard.setId(50L);
            updatedCard.setName("Dragon");
            updatedCard.setRarity(Rarity.EPIC);
            updatedCard.setDropRate(0.2);
            updatedCard.setCreatedBy(alice);
            updatedCard.setImageUrl("alice/50_dragon.png");

            when(repositoryCard.save(any(Card.class)))
                    .thenReturn(savedCard)     // first save (create)
                    .thenReturn(updatedCard);   // second save (with imageUrl)

            MockMultipartFile image = new MockMultipartFile(
                    "image", "dragon.png", "image/png", new byte[]{1, 2, 3, 4});

            CardDto dto = new CardDto(null, "Dragon", null, Rarity.EPIC, null, 0.2, false, null, alice.getId(), null);
            CardDto result = serviceCard.createCardWithImage(dto, image, "alice");

            assertEquals("alice/50_dragon.png", result.imageUrl());
            verify(repositoryCard, times(2)).save(any(Card.class));

            // Vérifier que le fichier a été écrit sur disque
            Path expectedFile = Paths.get(cardImagesDir, "alice", "50_dragon.png");
            assertTrue(Files.exists(expectedFile));
        }

        @Test
        @DisplayName("createCardWithImage rejects invalid content type")
        void testCreateCardWithImage_invalidType() {
            MockMultipartFile image = new MockMultipartFile(
                    "image", "file.txt", "text/plain", new byte[]{1, 2, 3});

            CardDto dto = new CardDto(null, "Bad", null, Rarity.COMMON, null, 1.0, false, null, alice.getId(), null);

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> serviceCard.createCardWithImage(dto, image, "alice")
            );
            assertTrue(ex.getMessage().contains("Invalid image type"));
        }

        @Test
        @DisplayName("createCardWithImage rejects file exceeding 5MB")
        void testCreateCardWithImage_tooLarge() {
            byte[] largeContent = new byte[6 * 1024 * 1024]; // 6MB
            MockMultipartFile image = new MockMultipartFile(
                    "image", "big.png", "image/png", largeContent);

            CardDto dto = new CardDto(null, "Big", null, Rarity.COMMON, null, 1.0, false, null, alice.getId(), null);

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> serviceCard.createCardWithImage(dto, image, "alice")
            );
            assertTrue(ex.getMessage().contains("too large"));
        }

        @Test
        @DisplayName("createCardWithImage rejects empty file")
        void testCreateCardWithImage_emptyFile() {
            MockMultipartFile image = new MockMultipartFile(
                    "image", "empty.png", "image/png", new byte[0]);

            CardDto dto = new CardDto(null, "Empty", null, Rarity.COMMON, null, 1.0, false, null, alice.getId(), null);

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> serviceCard.createCardWithImage(dto, image, "alice")
            );
            assertTrue(ex.getMessage().contains("required"));
        }

        @Test
        @DisplayName("sanitizeFilename removes special characters")
        void testSanitizeFilename() {
            assertEquals("hello_world.png", ImageUtils.sanitizeFilename("hello world.png"));
            assertEquals("file__test_.jpg", ImageUtils.sanitizeFilename("file (test).jpg"));
            assertEquals("image", ImageUtils.sanitizeFilename(null));
            assertEquals("image", ImageUtils.sanitizeFilename(""));
            assertEquals("normal-file_name.webp", ImageUtils.sanitizeFilename("normal-file_name.webp"));
        }
    }
}
