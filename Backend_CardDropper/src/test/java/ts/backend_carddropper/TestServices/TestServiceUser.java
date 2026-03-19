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
import ts.backend_carddropper.repository.RepositoryLiveFeed;
import ts.backend_carddropper.repository.RepositoryUser;
import ts.backend_carddropper.service.ServiceUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    @MockitoBean
    private RepositoryLiveFeed repositoryLiveFeed;

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

        // Mock par défaut pour le live feed (déclenché lors de useCard)
        when(repositoryLiveFeed.save(any(ts.backend_carddropper.entity.LiveFeedEvent.class)))
                .thenAnswer(inv -> {
                    ts.backend_carddropper.entity.LiveFeedEvent e = inv.getArgument(0);
                    e.setId(1L);
                    e.setCreatedAt(java.time.LocalDateTime.now());
                    return e;
                });
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

            UserDto dto = new UserDto(null, null, "alice", "alice@test.com", false, null, null, null);
            UserDto result = serviceUser.create(dto);

            assertNotNull(result);
            assertEquals("alice", result.username());
            assertEquals("alice@test.com", result.email());
        }

        @Test
        @DisplayName("create user fails when username is already taken")
        void testCreate_duplicateUsername() {
            when(repositoryUser.existsByUsername("alice")).thenReturn(true);

            UserDto dto = new UserDto(null, null, "alice", "alice@test.com", false, null, null, null);

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

            UserDto dto = new UserDto(null, null, "alice", "alice@test.com", false, null, null, null);

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

            UserDto aliceDto = new UserDto(alice.getId(), null, "Alice_updated", alice.getEmail(), false, null, null, null);
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

            UserDto dto = new UserDto(999L, null, "ghost", "ghost@test.com", false, null, null, null);
            Optional<UserDto> result = serviceUser.update(999L, dto);

            assertTrue(result.isEmpty());
            verify(repositoryUser, never()).save(any());
        }

        @Test
        @DisplayName("delete user detaches cards then deletes")
        void testDelete_success() {
            // Give alice an owned card via ManyToMany
            Card ownedCard = cards.getFirst();
            alice.getCardsOwned().add(ownedCard);

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            // Cards created by alice
            when(repositoryCard.findAllByCreatedById(alice.getId())).thenReturn(List.of(ownedCard));
            // Cards targeting alice
            when(repositoryCard.findAllByTargetUserId(alice.getId())).thenReturn(Collections.emptyList());

            serviceUser.delete(alice.getId());

            // ManyToMany ownership should be cleared
            assertTrue(alice.getCardsOwned().isEmpty());
            // Creator FK should be detached
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
            alice.getCardsOwned().add(card1);
            alice.getCardsOwned().add(card2);

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));

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
        @DisplayName("tradeCard swaps ownership between two users")
        void testTradeCard_success() {
            Card aliceCard = cards.get(0);
            Card bobCard = cards.get(12); // first RARE card
            // Set up ownership via collections
            alice.getCardsOwned().add(aliceCard);
            bob.getCardsOwned().add(bobCard);

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryUser.findById(bob.getId())).thenReturn(Optional.of(bob));
            when(repositoryUser.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            serviceUser.tradeCard(alice.getId(), aliceCard.getId(), bob.getId(), bobCard.getId());

            // After trade: alice no longer owns aliceCard, bob does
            assertFalse(alice.getCardsOwned().contains(aliceCard));
            assertTrue(bob.getCardsOwned().contains(aliceCard));
            // And vice versa
            assertFalse(bob.getCardsOwned().contains(bobCard));
            assertTrue(alice.getCardsOwned().contains(bobCard));
            verify(repositoryUser, times(2)).save(any(User.class));
        }

        @Test
        @DisplayName("tradeCard fails when user doesn't own the card")
        void testTradeCard_notOwned() {
            Card card = cards.get(0);
            // card is NOT in alice's collection — owned by nobody or bob
            bob.getCardsOwned().add(card);

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryUser.findById(bob.getId())).thenReturn(Optional.of(bob));

            assertThrows(IllegalArgumentException.class,
                    () -> serviceUser.tradeCard(alice.getId(), card.getId(), bob.getId(), cards.get(12).getId()));
        }

        @Test
        @DisplayName("openPack assigns pool cards to user")
        void testOpenPack_success() {
            Card poolCard1 = cards.get(0);
            Card poolCard2 = cards.get(1);
            // Both are pool cards (owners is empty)
            assertTrue(poolCard1.getOwners().isEmpty());
            assertTrue(poolCard2.getOwners().isEmpty());

            List<Long> cardIds = List.of(poolCard1.getId(), poolCard2.getId());

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryCard.findAllById(cardIds)).thenReturn(List.of(poolCard1, poolCard2));
            when(repositoryUser.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            List<CardDto> result = serviceUser.openPack(alice.getId(), cardIds);

            assertEquals(2, result.size());
            // Cards should now be in alice's collection
            assertTrue(alice.getCardsOwned().contains(poolCard1));
            assertTrue(alice.getCardsOwned().contains(poolCard2));
        }

        @Test
        @DisplayName("openPack fails when unique card is already owned")
        void testOpenPack_cardAlreadyOwned() {
            Card ownedCard = cards.get(0);
            ownedCard.setUniqueCard(true);
            ownedCard.addOwner(bob); // already owned — unique card with owner

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
            alice.getCardsOwned().add(c1);
            alice.getCardsOwned().add(c2);
            alice.getCardsOwned().add(c3);

            List<Long> cardIds = List.of(c1.getId(), c2.getId(), c3.getId());

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryCard.findAllById(cardIds)).thenReturn(List.of(c1, c2, c3));

            // Pool of RARE cards available for the result
            Card rarePoolCard = cards.get(12); // first RARE card, still in pool
            when(repositoryCard.findPoolCardsByRarity(Rarity.RARE)).thenReturn(List.of(rarePoolCard));
            when(repositoryUser.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            CardDto result = serviceUser.mergeCards(alice.getId(), cardIds);

            assertNotNull(result);
            assertEquals(Rarity.RARE, result.rarity());
            // Consumed cards should be removed from alice's collection
            assertFalse(alice.getCardsOwned().contains(c1));
            assertFalse(alice.getCardsOwned().contains(c2));
            assertFalse(alice.getCardsOwned().contains(c3));
            // Result card should be owned by alice
            assertTrue(alice.getCardsOwned().contains(rarePoolCard));
        }

        @Test
        @DisplayName("merge 3 RARE cards returns an EPIC card")
        void testMergeCards_rareToEpic() {
            Card r1 = cards.get(12);
            Card r2 = cards.get(13);
            Card r3 = cards.get(14);
            alice.getCardsOwned().add(r1);
            alice.getCardsOwned().add(r2);
            alice.getCardsOwned().add(r3);

            List<Long> cardIds = List.of(r1.getId(), r2.getId(), r3.getId());

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryCard.findAllById(cardIds)).thenReturn(List.of(r1, r2, r3));

            Card epicPoolCard = cards.get(18); // first EPIC card
            when(repositoryCard.findPoolCardsByRarity(Rarity.EPIC)).thenReturn(List.of(epicPoolCard));
            when(repositoryUser.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            CardDto result = serviceUser.mergeCards(alice.getId(), cardIds);

            assertEquals(Rarity.EPIC, result.rarity());
            assertTrue(alice.getCardsOwned().contains(epicPoolCard));
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
            alice.getCardsOwned().add(common);
            alice.getCardsOwned().add(rare);
            alice.getCardsOwned().add(epic);

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
            alice.getCardsOwned().add(c1);
            alice.getCardsOwned().add(c2);
            // c3 NOT in alice's collection

            List<Long> cardIds = List.of(c1.getId(), c2.getId(), c3.getId());

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryCard.findAllById(cardIds)).thenReturn(List.of(c1, c2, c3));

            assertThrows(IllegalArgumentException.class,
                    () -> serviceUser.mergeCards(alice.getId(), cardIds));
        }

        @Test
        @DisplayName("merge LEGENDARY cards throws — cannot merge further")
        void testMergeCards_legendaryCannotMerge() {
            Card l1 = cards.get(22);
            Card l2 = cards.get(23);
            Card l3 = new Card();
            l3.setId(99L);
            l3.setRarity(Rarity.LEGENDARY);

            alice.getCardsOwned().add(l1);
            alice.getCardsOwned().add(l2);
            alice.getCardsOwned().add(l3);

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
            alice.getCardsOwned().add(c1);
            alice.getCardsOwned().add(c2);
            alice.getCardsOwned().add(c3);

            List<Long> cardIds = List.of(c1.getId(), c2.getId(), c3.getId());

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryCard.findAllById(cardIds)).thenReturn(List.of(c1, c2, c3));
            when(repositoryCard.findPoolCardsByRarity(Rarity.RARE)).thenReturn(Collections.emptyList());

            assertThrows(IllegalStateException.class,
                    () -> serviceUser.mergeCards(alice.getId(), cardIds));
        }
    }


    // ========================================
    //         UTILISER UNE CARTE
    // ========================================

    @Nested
    @DisplayName("Utilisation de carte (useCard)")
    class UseCardTests {

        @Test
        @DisplayName("carte unique — consommée (retirée de la collection) et événement persisté")
        void testUseCard_uniqueCard_consumed() {
            Card card = cards.get(0);
            card.setUniqueCard(true);
            alice.getCardsOwned().add(card);

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryUser.findById(bob.getId())).thenReturn(Optional.of(bob));
            when(repositoryUser.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            serviceUser.useCard(alice.getId(), card.getId(), bob.getId());

            // Carte unique → retirée de la collection
            assertFalse(alice.getCardsOwned().contains(card));
            verify(repositoryUser).save(alice);

            // Vérifier que l'événement a été persisté via ServiceLiveFeed
            var captor = org.mockito.ArgumentCaptor.forClass(ts.backend_carddropper.entity.LiveFeedEvent.class);
            verify(repositoryLiveFeed).save(captor.capture());

            var savedEvent = captor.getValue();
            assertEquals("alice", savedEvent.getActorUsername());
            assertEquals(card.getName(), savedEvent.getCardName());
            assertEquals(Rarity.COMMON.name(), savedEvent.getCardRarity());
            assertEquals("bob", savedEvent.getTargetUsername());
        }

        @Test
        @DisplayName("carte non-unique — retirée de la collection après utilisation et événement persisté")
        void testUseCard_nonUniqueCard_consumed() {
            Card card = cards.get(0);
            card.setUniqueCard(false);
            alice.getCardsOwned().add(card);

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryUser.findById(bob.getId())).thenReturn(Optional.of(bob));
            when(repositoryUser.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            serviceUser.useCard(alice.getId(), card.getId(), bob.getId());

            // Carte non-unique → également retirée après utilisation
            assertFalse(alice.getCardsOwned().contains(card));
            verify(repositoryUser).save(alice);

            // L'événement est persisté
            verify(repositoryLiveFeed).save(any(ts.backend_carddropper.entity.LiveFeedEvent.class));
        }

        @Test
        @DisplayName("échoue quand l'utilisateur n'existe pas")
        void testUseCard_userNotFound() {
            when(repositoryUser.findById(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> serviceUser.useCard(999L, 1L, bob.getId()));
        }

        @Test
        @DisplayName("échoue quand la carte n'appartient pas à l'utilisateur")
        void testUseCard_cardNotOwned() {
            Card card = cards.get(0);
            // card NOT in alice's collection (owned by bob or nobody)

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));

            assertThrows(IllegalArgumentException.class,
                    () -> serviceUser.useCard(alice.getId(), card.getId(), bob.getId()));
        }

        @Test
        @DisplayName("échoue quand la carte cible un autre utilisateur que celui spécifié")
        void testUseCard_wrongTarget() {
            Card card = cards.get(0);
            alice.getCardsOwned().add(card);
            // La carte cible alice, pas bob
            card.setTargetUser(alice);

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryUser.findById(bob.getId())).thenReturn(Optional.of(bob));

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> serviceUser.useCard(alice.getId(), card.getId(), bob.getId())
            );
            assertTrue(ex.getMessage().contains("targets user id="));
        }

        @Test
        @DisplayName("réussit quand la carte unique cible le bon utilisateur")
        void testUseCard_correctTarget() {
            Card card = cards.get(0);
            card.setUniqueCard(true);
            card.setTargetUser(bob);
            alice.getCardsOwned().add(card);

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryUser.findById(bob.getId())).thenReturn(Optional.of(bob));
            when(repositoryUser.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            serviceUser.useCard(alice.getId(), card.getId(), bob.getId());

            // Carte unique → retirée de la collection
            assertFalse(alice.getCardsOwned().contains(card));
            verify(repositoryUser).save(alice);
        }

        @Test
        @DisplayName("échoue quand la cible n'existe pas")
        void testUseCard_targetNotFound() {
            Card card = cards.get(0);
            alice.getCardsOwned().add(card);

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryUser.findById(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> serviceUser.useCard(alice.getId(), card.getId(), 999L));
        }

        @Test
        @DisplayName("échoue quand la carte n'est pas dans la collection de l'utilisateur")
        void testUseCard_cardNotInCollection() {
            // Card exists but alice doesn't own it
            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));

            assertThrows(IllegalArgumentException.class,
                    () -> serviceUser.useCard(alice.getId(), 999L, bob.getId()));
        }
    }
}
