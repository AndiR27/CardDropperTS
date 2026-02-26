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
import ts.backend_carddropper.entity.User;
import ts.backend_carddropper.enums.Rarity;
import ts.backend_carddropper.helper.TestDataHelper;
import ts.backend_carddropper.mapping.MapperUser;
import ts.backend_carddropper.models.CardDto;
import ts.backend_carddropper.models.UserDto;
import ts.backend_carddropper.repository.RepositoryCard;
import ts.backend_carddropper.repository.RepositoryUser;
import ts.backend_carddropper.service.ServiceUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class TestServiceUser {

    @Autowired
    private ServiceUser serviceUser;

    @Autowired
    private TestDataHelper testDataHelper;

    @Autowired
    private MapperUser mapperUser;

    @MockitoBean
    private RepositoryUser repositoryUser;

    @MockitoBean
    private RepositoryCard repositoryCard;

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
    //              CRUD TESTS
    // ========================================

    @Nested
    @DisplayName("CRUD operations")
    class CrudTests {

        @Test
        @DisplayName("create user successfully when username and email are unique")
        void testCreate_success() {
            when(repositoryUser.existsByUsername(alice.getUsername())).thenReturn(false);
            when(repositoryUser.existsByEmail(alice.getEmail())).thenReturn(false);
            when(repositoryUser.save(any(User.class))).thenReturn(alice);

            UserDto dto = new UserDto(null, "alice", "alice@test.com", "hash_alice", null, null, null);
            UserDto result = serviceUser.create(dto);

            assertNotNull(result);
            assertEquals("alice", result.username());
            assertEquals("alice@test.com", result.email());
        }

        @Test
        @DisplayName("create user fails when username is already taken")
        void testCreate_duplicateUsername() {
            when(repositoryUser.existsByUsername("alice")).thenReturn(true);

            UserDto dto = new UserDto(null, "alice", "alice@test.com", "hash_alice", null, null, null);

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> serviceUser.create(dto)
            );
            assertTrue(ex.getMessage().contains("Username already taken"));
        }

        @Test
        @DisplayName("create user fails when email is already in use")
        void testCreate_duplicateEmail() {
            when(repositoryUser.existsByUsername("alice")).thenReturn(false);
            when(repositoryUser.existsByEmail("alice@test.com")).thenReturn(true);

            UserDto dto = new UserDto(null, "alice", "alice@test.com", "hash_alice", null, null, null);

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> serviceUser.create(dto)
            );
            assertTrue(ex.getMessage().contains("Email already in use"));
        }

        @Test
        @DisplayName("findById returns user when exists")
        void testFindById_exists() {
            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));

            Optional<UserDto> result = serviceUser.findById(alice.getId());

            assertTrue(result.isPresent());
            assertEquals("alice", result.get().username());
        }

        @Test
        @DisplayName("findById returns empty when user does not exist")
        void testFindById_notFound() {
            when(repositoryUser.findById(999L)).thenReturn(Optional.empty());

            Optional<UserDto> result = serviceUser.findById(999L);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("findAll returns all users")
        void testFindAll() {
            when(repositoryUser.findAll()).thenReturn(users);

            List<UserDto> result = serviceUser.findAll();

            assertEquals(2, result.size());
            assertEquals("alice", result.getFirst().username());
            assertEquals("bob", result.get(1).username());
        }

        @Test
        @DisplayName("findByUsername returns user when exists")
        void testFindByUsername_exists() {
            when(repositoryUser.findByUsername("bob")).thenReturn(Optional.of(bob));

            Optional<UserDto> result = serviceUser.findByUsername("bob");

            assertTrue(result.isPresent());
            assertEquals("bob@test.com", result.get().email());
        }

        @Test
        @DisplayName("findByEmail returns user when exists")
        void testFindByEmail_exists() {
            when(repositoryUser.findByEmail("alice@test.com")).thenReturn(Optional.of(alice));

            Optional<UserDto> result = serviceUser.findByEmail("alice@test.com");

            assertTrue(result.isPresent());
            assertEquals("alice", result.get().username());
        }

        @Test
        @DisplayName("update user successfully")
        void testUpdate_success() {
            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryUser.save(any(User.class))).thenReturn(alice);

            UserDto aliceDto = new UserDto(alice.getId(), "Alice_updated", alice.getEmail(), alice.getPasswordHash(), null, null, null);
            Optional<UserDto> resultOpt = serviceUser.update(alice.getId(), aliceDto);

            assertTrue(resultOpt.isPresent());
            assertEquals("Alice_updated", resultOpt.get().username());
            verify(repositoryUser).findById(1L);
            verify(repositoryUser).save(any(User.class));
        }

        @Test
        @DisplayName("update returns empty when user not found")
        void testUpdate_notFound() {
            when(repositoryUser.findById(999L)).thenReturn(Optional.empty());

            UserDto dto = new UserDto(999L, "ghost", "ghost@test.com", "hash", null, null, null);
            Optional<UserDto> result = serviceUser.update(999L, dto);

            assertTrue(result.isEmpty());
            verify(repositoryUser, never()).save(any());
        }

        @Test
        @DisplayName("delete user detaches cards then deletes")
        void testDelete_success() {
            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            // Cards owned by alice
            Card ownedCard = cards.getFirst();
            ownedCard.setUser(alice);
            when(repositoryCard.findAllByUserId(alice.getId())).thenReturn(List.of(ownedCard));
            // Cards created by alice
            when(repositoryCard.findAllByCreatedById(alice.getId())).thenReturn(List.of(ownedCard));
            // Cards targeting alice
            when(repositoryCard.findAllByTargetUserId(alice.getId())).thenReturn(Collections.emptyList());

            serviceUser.delete(alice.getId());

            // Card relations should be detached
            assertNull(ownedCard.getUser());
            assertNull(ownedCard.getCreatedBy());
            verify(repositoryCard).flush();
            verify(repositoryUser).delete(alice);
        }

        @Test
        @DisplayName("delete throws when user not found")
        void testDelete_notFound() {
            when(repositoryUser.findById(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> serviceUser.delete(999L));
            verify(repositoryUser, never()).delete(any());
        }
    }


    // ========================================
    //         USER-CARD RELATION TESTS
    // ========================================

    @Nested
    @DisplayName("User-Card relations")
    class UserCardTests {

        @Test
        @DisplayName("getCardsOwned returns cards belonging to user")
        void testGetCardsOwned_success() {
            Card card1 = cards.get(0);
            Card card2 = cards.get(1);
            card1.setUser(alice);
            card2.setUser(alice);

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryCard.findAllByUserId(alice.getId())).thenReturn(List.of(card1, card2));

            List<CardDto> result = serviceUser.getCardsOwned(alice.getId());

            assertEquals(2, result.size());
            assertEquals(card1.getName(), result.getFirst().name());
            assertEquals(card2.getName(), result.get(1).name());
        }

        @Test
        @DisplayName("getCardsOwned throws when user not found")
        void testGetCardsOwned_userNotFound() {
            when(repositoryUser.findById(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> serviceUser.getCardsOwned(999L));
        }

        @Test
        @DisplayName("createCard adds card to pool with creator set, no owner")
        void testCreateCard_success() {
            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));

            Card savedCard = new Card();
            savedCard.setId(100L);
            savedCard.setName("NewCard");
            savedCard.setRarity(Rarity.RARE);
            savedCard.setDropRate(0.5);
            savedCard.setCreatedBy(alice);
            savedCard.setUser(null);

            when(repositoryCard.save(any(Card.class))).thenReturn(savedCard);

            CardDto dto = new CardDto(null, "NewCard", null, Rarity.RARE, null, 0.5, false, null, alice.getId(), null);
            CardDto result = serviceUser.createCard(alice.getId(), dto);

            assertNotNull(result);
            assertEquals("NewCard", result.name());
            assertNull(result.userId(), "Card should have no owner (pool card)");
            assertEquals(alice.getId(), result.createdById());
        }

        @Test
        @DisplayName("createCard with targetUser set to self succeeds")
        void testCreateCard_withTargetSelf() {
            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));

            Card savedCard = new Card();
            savedCard.setId(101L);
            savedCard.setName("SelfTarget");
            savedCard.setRarity(Rarity.COMMON);
            savedCard.setDropRate(1.0);
            savedCard.setCreatedBy(alice);
            savedCard.setTargetUser(alice);
            savedCard.setUser(null);

            when(repositoryCard.save(any(Card.class))).thenReturn(savedCard);

            CardDto dto = new CardDto(null, "SelfTarget", null, Rarity.COMMON, null, 1.0, false, null, alice.getId(), alice.getId());
            CardDto result = serviceUser.createCard(alice.getId(), dto);

            assertEquals(alice.getId(), result.targetUserId());
            assertNull(result.userId());
        }

        @Test
        @DisplayName("createCard fails when targetUser is not the creator")
        void testCreateCard_targetNotCreator() {
            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));

            CardDto dto = new CardDto(null, "BadTarget", null, Rarity.COMMON, null, 1.0, false, null, alice.getId(), bob.getId());

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> serviceUser.createCard(alice.getId(), dto)
            );
            assertTrue(ex.getMessage().contains("targetUser must be the card creator itself"));
        }

        @Test
        @DisplayName("tradeCard swaps ownership between two users")
        void testTradeCard_success() {
            Card aliceCard = cards.get(0);
            aliceCard.setUser(alice);
            Card bobCard = cards.get(12); // first RARE card
            bobCard.setUser(bob);

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryUser.findById(bob.getId())).thenReturn(Optional.of(bob));
            when(repositoryCard.findById(aliceCard.getId())).thenReturn(Optional.of(aliceCard));
            when(repositoryCard.findById(bobCard.getId())).thenReturn(Optional.of(bobCard));
            when(repositoryCard.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

            serviceUser.tradeCard(alice.getId(), aliceCard.getId(), bob.getId(), bobCard.getId());

            // After trade: alice's card now belongs to bob, and vice versa
            assertEquals(bob, aliceCard.getUser());
            assertEquals(alice, bobCard.getUser());
            verify(repositoryCard, times(2)).save(any(Card.class));
        }

        @Test
        @DisplayName("tradeCard fails when user doesn't own the card")
        void testTradeCard_notOwned() {
            Card card = cards.get(0);
            card.setUser(bob); // owned by bob, not alice

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryUser.findById(bob.getId())).thenReturn(Optional.of(bob));
            when(repositoryCard.findById(card.getId())).thenReturn(Optional.of(card));

            assertThrows(IllegalArgumentException.class,
                    () -> serviceUser.tradeCard(alice.getId(), card.getId(), bob.getId(), cards.get(12).getId()));
        }

        @Test
        @DisplayName("openPack assigns pool cards to user")
        void testOpenPack_success() {
            Card poolCard1 = cards.get(0);
            Card poolCard2 = cards.get(1);
            // Both are pool cards (user == null)
            assertNull(poolCard1.getUser());
            assertNull(poolCard2.getUser());

            List<Long> cardIds = List.of(poolCard1.getId(), poolCard2.getId());

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryCard.findAllById(cardIds)).thenReturn(List.of(poolCard1, poolCard2));
            when(repositoryCard.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

            List<CardDto> result = serviceUser.openPack(alice.getId(), cardIds);

            assertEquals(2, result.size());
            // Cards should now be owned by alice
            assertEquals(alice, poolCard1.getUser());
            assertEquals(alice, poolCard2.getUser());
        }

        @Test
        @DisplayName("openPack fails when card is already owned")
        void testOpenPack_cardAlreadyOwned() {
            Card ownedCard = cards.get(0);
            ownedCard.setUser(bob); // already owned

            List<Long> cardIds = List.of(ownedCard.getId());

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryCard.findAllById(cardIds)).thenReturn(List.of(ownedCard));

            assertThrows(IllegalStateException.class,
                    () -> serviceUser.openPack(alice.getId(), cardIds));
        }

        @Test
        @DisplayName("openPack fails when some cards not found")
        void testOpenPack_cardNotFound() {
            List<Long> cardIds = List.of(1L, 2L, 999L);

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            // Only 2 of 3 found
            when(repositoryCard.findAllById(cardIds)).thenReturn(List.of(cards.get(0), cards.get(1)));

            assertThrows(EntityNotFoundException.class,
                    () -> serviceUser.openPack(alice.getId(), cardIds));
        }
    }


    // ========================================
    //            MERGE CARDS TESTS
    // ========================================

    @Nested
    @DisplayName("Merge cards")
    class MergeTests {

        @Test
        @DisplayName("merge 3 COMMON cards returns a RARE card")
        void testMergeCards_commonToRare() {
            // Give alice 3 common cards
            Card c1 = cards.get(0);
            Card c2 = cards.get(1);
            Card c3 = cards.get(2);
            c1.setUser(alice);
            c2.setUser(alice);
            c3.setUser(alice);

            List<Long> cardIds = List.of(c1.getId(), c2.getId(), c3.getId());

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryCard.findAllById(cardIds)).thenReturn(List.of(c1, c2, c3));

            // Pool of RARE cards available for the result
            Card rarePoolCard = cards.get(12); // first RARE card, still in pool (user=null)
            when(repositoryCard.findByRarityAndUserIsNull(Rarity.RARE)).thenReturn(List.of(rarePoolCard));
            when(repositoryCard.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
            when(repositoryCard.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

            CardDto result = serviceUser.mergeCards(alice.getId(), cardIds);

            assertNotNull(result);
            assertEquals(Rarity.RARE, result.rarity());
            // Consumed cards should be returned to pool
            assertNull(c1.getUser());
            assertNull(c2.getUser());
            assertNull(c3.getUser());
            // Result card should be owned by alice
            assertEquals(alice, rarePoolCard.getUser());
        }

        @Test
        @DisplayName("merge 3 RARE cards returns an EPIC card")
        void testMergeCards_rareToEpic() {
            Card r1 = cards.get(12);
            Card r2 = cards.get(13);
            Card r3 = cards.get(14);
            r1.setUser(alice);
            r2.setUser(alice);
            r3.setUser(alice);

            List<Long> cardIds = List.of(r1.getId(), r2.getId(), r3.getId());

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryCard.findAllById(cardIds)).thenReturn(List.of(r1, r2, r3));

            Card epicPoolCard = cards.get(18); // first EPIC card
            when(repositoryCard.findByRarityAndUserIsNull(Rarity.EPIC)).thenReturn(List.of(epicPoolCard));
            when(repositoryCard.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
            when(repositoryCard.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

            CardDto result = serviceUser.mergeCards(alice.getId(), cardIds);

            assertEquals(Rarity.EPIC, result.rarity());
            assertEquals(alice, epicPoolCard.getUser());
        }

        @Test
        @DisplayName("merge fails when not exactly 3 cards provided")
        void testMergeCards_wrongCount() {
            List<Long> twoCards = List.of(1L, 2L);

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> serviceUser.mergeCards(alice.getId(), twoCards)
            );
            assertTrue(ex.getMessage().contains("Exactly 3 cards are required"));
        }

        @Test
        @DisplayName("merge fails when cards have mixed rarities")
        void testMergeCards_mixedRarities() {
            Card common = cards.get(0);
            Card rare = cards.get(12);
            Card epic = cards.get(18);
            common.setUser(alice);
            rare.setUser(alice);
            epic.setUser(alice);

            List<Long> cardIds = List.of(common.getId(), rare.getId(), epic.getId());

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryCard.findAllById(cardIds)).thenReturn(List.of(common, rare, epic));

            assertThrows(IllegalArgumentException.class,
                    () -> serviceUser.mergeCards(alice.getId(), cardIds));
        }

        @Test
        @DisplayName("merge fails when user doesn't own one of the cards")
        void testMergeCards_notOwned() {
            Card c1 = cards.get(0);
            Card c2 = cards.get(1);
            Card c3 = cards.get(2);
            c1.setUser(alice);
            c2.setUser(alice);
            c3.setUser(bob); // not owned by alice

            List<Long> cardIds = List.of(c1.getId(), c2.getId(), c3.getId());

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryCard.findAllById(cardIds)).thenReturn(List.of(c1, c2, c3));

            assertThrows(IllegalArgumentException.class,
                    () -> serviceUser.mergeCards(alice.getId(), cardIds));
        }

        @Test
        @DisplayName("merge LENGENDARY cards throws — cannot merge further")
        void testMergeCards_legendaryCannotMerge() {
            Card l1 = cards.get(22);
            Card l2 = cards.get(23);
            // Need a 3rd legendary — reuse l1 concept with different id
            Card l3 = new Card();
            l3.setId(99L);
            l3.setRarity(Rarity.LEGENDARY);
            l3.setUser(alice);

            l1.setUser(alice);
            l2.setUser(alice);

            List<Long> cardIds = List.of(l1.getId(), l2.getId(), l3.getId());

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryCard.findAllById(cardIds)).thenReturn(List.of(l1, l2, l3));

            assertThrows(IllegalArgumentException.class,
                    () -> serviceUser.mergeCards(alice.getId(), cardIds));
        }

        @Test
        @DisplayName("merge fails when no card of next rarity available in pool")
        void testMergeCards_emptyPool() {
            Card c1 = cards.get(0);
            Card c2 = cards.get(1);
            Card c3 = cards.get(2);
            c1.setUser(alice);
            c2.setUser(alice);
            c3.setUser(alice);

            List<Long> cardIds = List.of(c1.getId(), c2.getId(), c3.getId());

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryCard.findAllById(cardIds)).thenReturn(List.of(c1, c2, c3));
            when(repositoryCard.findByRarityAndUserIsNull(Rarity.RARE)).thenReturn(Collections.emptyList());

            assertThrows(IllegalStateException.class,
                    () -> serviceUser.mergeCards(alice.getId(), cardIds));
        }
    }
}
