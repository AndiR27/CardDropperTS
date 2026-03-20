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
import ts.backend_carddropper.entity.UserCard;
import ts.backend_carddropper.enums.Rarity;
import ts.backend_carddropper.helper.TestDataHelper;
import ts.backend_carddropper.mapping.MapperUser;
import ts.backend_carddropper.models.CardDto;
import ts.backend_carddropper.models.UserDto;
import ts.backend_carddropper.repository.RepositoryCard;
import ts.backend_carddropper.repository.RepositoryLiveFeed;
import ts.backend_carddropper.repository.RepositoryUser;
import ts.backend_carddropper.repository.RepositoryUserCard;
import ts.backend_carddropper.service.ServiceUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
    private RepositoryUserCard repositoryUserCard;

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

        // Default: repositoryUserCard.save returns the saved entity
        when(repositoryUserCard.save(any(UserCard.class))).thenAnswer(inv -> inv.getArgument(0));

        // Mock par défaut pour le live feed (déclenché lors de useCard)
        when(repositoryLiveFeed.save(any(ts.backend_carddropper.entity.LiveFeedEvent.class)))
                .thenAnswer(inv -> {
                    ts.backend_carddropper.entity.LiveFeedEvent e = inv.getArgument(0);
                    e.setId(1L);
                    e.setCreatedAt(java.time.LocalDateTime.now());
                    return e;
                });
    }

    /** Helper: simule qu'un user possède une carte (quantity=1) */
    private UserCard giveCard(User user, Card card) {
        UserCard uc = new UserCard(user, card, 1);
        when(repositoryUserCard.findByUserIdAndCardId(user.getId(), card.getId()))
                .thenReturn(Optional.of(uc));
        return uc;
    }

    /** Helper: simule qu'un user possède plusieurs copies d'une carte */
    private UserCard giveCards(User user, Card card, int quantity) {
        UserCard uc = new UserCard(user, card, quantity);
        when(repositoryUserCard.findByUserIdAndCardId(user.getId(), card.getId()))
                .thenReturn(Optional.of(uc));
        return uc;
    }

    /** Helper: simule qu'un user ne possède PAS une carte */
    private void noCard(User user, Card card) {
        when(repositoryUserCard.findByUserIdAndCardId(user.getId(), card.getId()))
                .thenReturn(Optional.empty());
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
            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryCard.findAllByCreatedById(alice.getId())).thenReturn(Collections.emptyList());
            when(repositoryCard.findAllByTargetUserId(alice.getId())).thenReturn(Collections.emptyList());

            serviceUser.delete(alice.getId());

            verify(repositoryUserCard).deleteByUserId(alice.getId());
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

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryUserCard.findByUserId(alice.getId()))
                    .thenReturn(List.of(new UserCard(alice, card1, 1), new UserCard(alice, card2, 1)));

            List<CardDto> result = serviceUser.getCardsOwned(alice.getId());

            assertEquals(2, result.size());
            assertEquals(card1.getName(), result.getFirst().name());
            assertEquals(card2.getName(), result.get(1).name());
        }

        @Test
        @DisplayName("getCardsOwned expands quantity — 2x same card returns 2 entries")
        void testGetCardsOwned_expandsQuantity() {
            Card card1 = cards.get(0);

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryUserCard.findByUserId(alice.getId()))
                    .thenReturn(List.of(new UserCard(alice, card1, 2)));

            List<CardDto> result = serviceUser.getCardsOwned(alice.getId());

            assertEquals(2, result.size());
            assertEquals(card1.getName(), result.getFirst().name());
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

            UserCard aliceUc = giveCard(alice, aliceCard);
            UserCard bobUc = giveCard(bob, bobCard);
            // alice doesn't have bobCard yet, bob doesn't have aliceCard yet
            noCard(alice, bobCard);
            noCard(bob, aliceCard);

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryUser.findById(bob.getId())).thenReturn(Optional.of(bob));

            serviceUser.tradeCard(alice.getId(), aliceCard.getId(), bob.getId(), bobCard.getId());

            // Both cards were removed from their owners (quantity=1 → delete)
            verify(repositoryUserCard, times(2)).delete(any(UserCard.class));
            // Each card added to the new owner (save new UserCard)
            verify(repositoryUserCard, times(2)).save(any(UserCard.class));
        }

        @Test
        @DisplayName("tradeCard fails when user doesn't own the card")
        void testTradeCard_notOwned() {
            Card card = cards.get(0);
            noCard(alice, card);

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
            List<Long> cardIds = List.of(poolCard1.getId(), poolCard2.getId());

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryCard.findAllById(cardIds)).thenReturn(List.of(poolCard1, poolCard2));
            // alice doesn't own either card yet
            noCard(alice, poolCard1);
            noCard(alice, poolCard2);

            List<CardDto> result = serviceUser.openPack(alice.getId(), cardIds);

            assertEquals(2, result.size());
            // Two new UserCards should be saved (one per card)
            verify(repositoryUserCard, times(2)).save(any(UserCard.class));
        }

        @Test
        @DisplayName("openPack succeeds with a unique card that is not yet owned")
        void testOpenPack_uniqueCardNotOwned() {
            Card uniqueCard = cards.get(0);
            uniqueCard.setUniqueCard(true);

            List<Long> cardIds = List.of(uniqueCard.getId());

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryCard.findAllById(cardIds)).thenReturn(List.of(uniqueCard));
            noCard(alice, uniqueCard);

            List<CardDto> result = serviceUser.openPack(alice.getId(), cardIds);

            assertEquals(1, result.size());
            verify(repositoryUserCard).save(any(UserCard.class));
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
            Card c1 = cards.get(0);
            Card c2 = cards.get(1);
            Card c3 = cards.get(2);
            giveCard(alice, c1);
            giveCard(alice, c2);
            giveCard(alice, c3);

            List<Long> cardIds = List.of(c1.getId(), c2.getId(), c3.getId());

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryCard.findAllById(cardIds)).thenReturn(List.of(c1, c2, c3));

            Card rarePoolCard = cards.get(12); // first RARE card
            when(repositoryCard.findPoolCardsByRarity(Rarity.RARE)).thenReturn(List.of(rarePoolCard));
            // alice doesn't have the result card yet
            noCard(alice, rarePoolCard);

            CardDto result = serviceUser.mergeCards(alice.getId(), cardIds);

            assertNotNull(result);
            assertEquals(Rarity.RARE, result.rarity());
            // All 3 consumed cards should be deleted (each has quantity=1)
            verify(repositoryUserCard, times(3)).delete(any(UserCard.class));
            // Result card should be saved as new UserCard
            verify(repositoryUserCard).save(argThat(uc -> uc.getCard().getId().equals(rarePoolCard.getId())));
        }

        @Test
        @DisplayName("merge 4 RARE cards returns an EPIC card")
        void testMergeCards_rareToEpic() {
            Card r1 = cards.get(12);
            Card r2 = cards.get(13);
            Card r3 = cards.get(14);
            Card r4 = cards.get(15);
            giveCard(alice, r1);
            giveCard(alice, r2);
            giveCard(alice, r3);
            giveCard(alice, r4);

            List<Long> cardIds = List.of(r1.getId(), r2.getId(), r3.getId(), r4.getId());

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryCard.findAllById(cardIds)).thenReturn(List.of(r1, r2, r3, r4));

            Card epicPoolCard = cards.get(18); // first EPIC card
            when(repositoryCard.findPoolCardsByRarity(Rarity.EPIC)).thenReturn(List.of(epicPoolCard));
            noCard(alice, epicPoolCard);

            CardDto result = serviceUser.mergeCards(alice.getId(), cardIds);

            assertEquals(Rarity.EPIC, result.rarity());
            verify(repositoryUserCard, times(4)).delete(any(UserCard.class));
            verify(repositoryUserCard).save(argThat(uc -> uc.getCard().getId().equals(epicPoolCard.getId())));
        }

        @Test
        @DisplayName("merge 3 copies of same card (duplicate IDs) reduces quantity correctly")
        void testMergeCards_triplicateCard() {
            Card c1 = cards.get(0);
            // alice has 3 copies of c1
            giveCards(alice, c1, 3);

            // All 3 IDs are the same
            List<Long> cardIds = List.of(c1.getId(), c1.getId(), c1.getId());

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            // findAllById returns distinct
            when(repositoryCard.findAllById(List.of(c1.getId()))).thenReturn(List.of(c1));

            Card rarePoolCard = cards.get(12);
            when(repositoryCard.findPoolCardsByRarity(Rarity.RARE)).thenReturn(List.of(rarePoolCard));
            noCard(alice, rarePoolCard);

            CardDto result = serviceUser.mergeCards(alice.getId(), cardIds);

            assertNotNull(result);
            assertEquals(Rarity.RARE, result.rarity());
            // c1 had quantity=3, all 3 consumed → deleted
            verify(repositoryUserCard).delete(any(UserCard.class));
        }

        @Test
        @DisplayName("merge fails when wrong number of cards provided for the rarity")
        void testMergeCards_wrongCount() {
            Card c1 = cards.get(0); // COMMON
            Card c2 = cards.get(1); // COMMON
            List<Long> twoCards = List.of(c1.getId(), c2.getId());

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryCard.findAllById(twoCards)).thenReturn(List.of(c1, c2));
            giveCard(alice, c1);
            giveCard(alice, c2);

            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> serviceUser.mergeCards(alice.getId(), twoCards)
            );
            assertTrue(ex.getMessage().contains("cards are required to merge"));
        }

        @Test
        @DisplayName("merge fails when cards have mixed rarities")
        void testMergeCards_mixedRarities() {
            Card common = cards.get(0);
            Card rare = cards.get(12);
            Card epic = cards.get(18);
            giveCard(alice, common);
            giveCard(alice, rare);
            giveCard(alice, epic);

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
            giveCard(alice, c1);
            giveCard(alice, c2);
            noCard(alice, c3); // c3 NOT owned

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

            giveCard(alice, l1);
            giveCard(alice, l2);
            giveCard(alice, l3);

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
            giveCard(alice, c1);
            giveCard(alice, c2);
            giveCard(alice, c3);

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
        @DisplayName("carte consommée et événement persisté")
        void testUseCard_uniqueCard_consumed() {
            Card card = cards.get(0);
            card.setUniqueCard(true);
            UserCard uc = giveCard(alice, card);

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryUser.findById(bob.getId())).thenReturn(Optional.of(bob));

            serviceUser.useCard(alice.getId(), card.getId(), bob.getId());

            // Carte retirée (quantity=1 → delete)
            verify(repositoryUserCard).delete(uc);

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
        @DisplayName("carte non-unique — retirée de la collection après utilisation")
        void testUseCard_nonUniqueCard_consumed() {
            Card card = cards.get(0);
            card.setUniqueCard(false);
            UserCard uc = giveCard(alice, card);

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryUser.findById(bob.getId())).thenReturn(Optional.of(bob));

            serviceUser.useCard(alice.getId(), card.getId(), bob.getId());

            verify(repositoryUserCard).delete(uc);
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
            noCard(alice, card);

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));

            assertThrows(IllegalArgumentException.class,
                    () -> serviceUser.useCard(alice.getId(), card.getId(), bob.getId()));
        }

        @Test
        @DisplayName("échoue quand la carte cible un autre utilisateur que celui spécifié")
        void testUseCard_wrongTarget() {
            Card card = cards.get(0);
            card.setTargetUser(alice); // la carte cible alice, pas bob
            giveCard(alice, card);

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
            UserCard uc = giveCard(alice, card);

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryUser.findById(bob.getId())).thenReturn(Optional.of(bob));

            serviceUser.useCard(alice.getId(), card.getId(), bob.getId());

            verify(repositoryUserCard).delete(uc);
        }

        @Test
        @DisplayName("échoue quand la cible n'existe pas")
        void testUseCard_targetNotFound() {
            Card card = cards.get(0);
            giveCard(alice, card);

            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryUser.findById(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> serviceUser.useCard(alice.getId(), card.getId(), 999L));
        }

        @Test
        @DisplayName("échoue quand la carte n'est pas dans la collection de l'utilisateur")
        void testUseCard_cardNotInCollection() {
            when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
            when(repositoryUserCard.findByUserIdAndCardId(eq(alice.getId()), anyLong()))
                    .thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                    () -> serviceUser.useCard(alice.getId(), 999L, bob.getId()));
        }
    }
}
