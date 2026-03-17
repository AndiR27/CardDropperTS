package ts.backend_carddropper.TestServices;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ts.backend_carddropper.entity.Card;
import ts.backend_carddropper.entity.User;
import ts.backend_carddropper.enums.Rarity;
import ts.backend_carddropper.helper.TestDataHelper;
import ts.backend_carddropper.repository.RepositoryCard;
import ts.backend_carddropper.repository.RepositoryUser;
import ts.backend_carddropper.trade.entity.TradeSession;
import ts.backend_carddropper.trade.enums.TradeSessionStatus;
import ts.backend_carddropper.trade.models.TradeSessionDto;
import ts.backend_carddropper.trade.repository.RepositoryTradeSession;
import ts.backend_carddropper.trade.service.ServiceTradeSession;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class TestServiceTradeSession {

    @Autowired
    private ServiceTradeSession serviceTradeSession;

    @Autowired
    private TestDataHelper testDataHelper;

    @MockitoBean
    private RepositoryTradeSession repositoryTradeSession;

    @MockitoBean
    private RepositoryUser repositoryUser;

    @MockitoBean
    private RepositoryCard repositoryCard;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    private User alice;
    private User bob;
    private List<Card> cards;

    private static final String ALICE_KEYCLOAK_ID = "kc-alice-001";
    private static final String BOB_KEYCLOAK_ID = "kc-bob-002";

    @BeforeEach
    void setUp() {
        List<User> users = testDataHelper.createUsers();
        alice = users.getFirst();
        alice.setKeycloakId(ALICE_KEYCLOAK_ID);
        bob = users.get(1);
        bob.setKeycloakId(BOB_KEYCLOAK_ID);
        cards = testDataHelper.createCards(alice);
    }

    private TradeSession buildSession(TradeSessionStatus status) {
        TradeSession session = new TradeSession();
        session.setId(UUID.randomUUID());
        session.setStatus(status);
        session.setInitiator(alice);
        session.setReceiver(bob);
        session.setInitiatorLocked(false);
        session.setReceiverLocked(false);
        session.setCreatedAt(LocalDateTime.now());
        return session;
    }

    private void mockUserLookups() {
        when(repositoryUser.findByKeycloakId(ALICE_KEYCLOAK_ID)).thenReturn(Optional.of(alice));
        when(repositoryUser.findByKeycloakId(BOB_KEYCLOAK_ID)).thenReturn(Optional.of(bob));
        when(repositoryUser.findById(alice.getId())).thenReturn(Optional.of(alice));
        when(repositoryUser.findById(bob.getId())).thenReturn(Optional.of(bob));
    }

    private void giveCardToUser(User user, Card card) {
        if (!user.getCardsOwned().contains(card)) {
            user.getCardsOwned().add(card);
        }
    }


    // ========================================
    //         CREATE SESSION TESTS
    // ========================================

    @Nested
    @DisplayName("createSession")
    class CreateSessionTests {

        @Test
        @DisplayName("creates session successfully with PENDING status")
        void testCreateSession_success() {
            mockUserLookups();
            when(repositoryTradeSession.findActiveSessionForUser(any())).thenReturn(Optional.empty());
            when(repositoryTradeSession.save(any())).thenAnswer(inv -> {
                TradeSession s = inv.getArgument(0);
                s.setId(UUID.randomUUID());
                return s;
            });

            TradeSessionDto result = serviceTradeSession.createSession(ALICE_KEYCLOAK_ID, bob.getId());

            assertNotNull(result);
            assertEquals(TradeSessionStatus.PENDING, result.status());
            assertEquals(alice.getUsername(), result.initiatorUsername());
            assertEquals(bob.getUsername(), result.receiverUsername());
        }

        @Test
        @DisplayName("notifies receiver via personal queue on creation")
        void testCreateSession_notifiesReceiver() {
            mockUserLookups();
            when(repositoryTradeSession.findActiveSessionForUser(any())).thenReturn(Optional.empty());
            when(repositoryTradeSession.save(any())).thenAnswer(inv -> {
                TradeSession s = inv.getArgument(0);
                s.setId(UUID.randomUUID());
                return s;
            });

            serviceTradeSession.createSession(ALICE_KEYCLOAK_ID, bob.getId());

            verify(messagingTemplate).convertAndSendToUser(
                    eq(bob.getUsername()), eq("/queue/trade-invites"), any(TradeSessionDto.class));
        }

        @Test
        @DisplayName("throws when trading with yourself")
        void testCreateSession_selfTrade() {
            mockUserLookups();

            assertThrows(IllegalArgumentException.class,
                    () -> serviceTradeSession.createSession(ALICE_KEYCLOAK_ID, alice.getId()));
        }

        @Test
        @DisplayName("throws when receiver not found")
        void testCreateSession_receiverNotFound() {
            mockUserLookups();
            when(repositoryUser.findById(999L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> serviceTradeSession.createSession(ALICE_KEYCLOAK_ID, 999L));
        }

        @Test
        @DisplayName("throws when initiator already has an active session")
        void testCreateSession_initiatorBusy() {
            mockUserLookups();
            when(repositoryTradeSession.findActiveSessionForUser(alice)).thenReturn(Optional.of(buildSession(TradeSessionStatus.ACTIVE)));

            assertThrows(IllegalStateException.class,
                    () -> serviceTradeSession.createSession(ALICE_KEYCLOAK_ID, bob.getId()));
        }

        @Test
        @DisplayName("throws when receiver already has an active session")
        void testCreateSession_receiverBusy() {
            mockUserLookups();
            when(repositoryTradeSession.findActiveSessionForUser(alice)).thenReturn(Optional.empty());
            when(repositoryTradeSession.findActiveSessionForUser(bob)).thenReturn(Optional.of(buildSession(TradeSessionStatus.ACTIVE)));

            assertThrows(IllegalStateException.class,
                    () -> serviceTradeSession.createSession(ALICE_KEYCLOAK_ID, bob.getId()));
        }
    }


    // ========================================
    //         JOIN SESSION TESTS
    // ========================================

    @Nested
    @DisplayName("joinSession")
    class JoinSessionTests {

        @Test
        @DisplayName("receiver joins and session becomes ACTIVE")
        void testJoinSession_success() {
            mockUserLookups();
            TradeSession session = buildSession(TradeSessionStatus.PENDING);
            when(repositoryTradeSession.findById(session.getId())).thenReturn(Optional.of(session));
            when(repositoryTradeSession.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TradeSessionDto result = serviceTradeSession.joinSession(session.getId(), BOB_KEYCLOAK_ID);

            assertEquals(TradeSessionStatus.ACTIVE, result.status());
            verify(messagingTemplate).convertAndSend(eq("/topic/trade/" + session.getId()), any(TradeSessionDto.class));
        }

        @Test
        @DisplayName("throws when non-receiver tries to join")
        void testJoinSession_wrongUser() {
            mockUserLookups();
            TradeSession session = buildSession(TradeSessionStatus.PENDING);
            when(repositoryTradeSession.findById(session.getId())).thenReturn(Optional.of(session));

            assertThrows(IllegalArgumentException.class,
                    () -> serviceTradeSession.joinSession(session.getId(), ALICE_KEYCLOAK_ID));
        }

        @Test
        @DisplayName("throws when session is not PENDING")
        void testJoinSession_notPending() {
            mockUserLookups();
            TradeSession session = buildSession(TradeSessionStatus.ACTIVE);
            when(repositoryTradeSession.findById(session.getId())).thenReturn(Optional.of(session));

            assertThrows(IllegalStateException.class,
                    () -> serviceTradeSession.joinSession(session.getId(), BOB_KEYCLOAK_ID));
        }
    }


    // ========================================
    //         SELECT CARD TESTS
    // ========================================

    @Nested
    @DisplayName("selectCard")
    class SelectCardTests {

        @Test
        @DisplayName("initiator selects a card successfully")
        void testSelectCard_initiator() {
            mockUserLookups();
            TradeSession session = buildSession(TradeSessionStatus.ACTIVE);
            Card card = cards.getFirst();
            giveCardToUser(alice, card);

            when(repositoryTradeSession.findById(session.getId())).thenReturn(Optional.of(session));
            when(repositoryCard.findById(card.getId())).thenReturn(Optional.of(card));
            when(repositoryTradeSession.isCardInActiveSessionForUser(alice, card.getId())).thenReturn(false);
            when(repositoryTradeSession.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TradeSessionDto result = serviceTradeSession.selectCard(session.getId(), ALICE_KEYCLOAK_ID, card.getId());

            assertNotNull(result.initiatorCard());
            assertEquals(card.getId(), result.initiatorCard().id());
        }

        @Test
        @DisplayName("receiver selects a card successfully")
        void testSelectCard_receiver() {
            mockUserLookups();
            TradeSession session = buildSession(TradeSessionStatus.ACTIVE);
            Card card = cards.get(1);
            giveCardToUser(bob, card);

            when(repositoryTradeSession.findById(session.getId())).thenReturn(Optional.of(session));
            when(repositoryCard.findById(card.getId())).thenReturn(Optional.of(card));
            when(repositoryTradeSession.isCardInActiveSessionForUser(bob, card.getId())).thenReturn(false);
            when(repositoryTradeSession.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TradeSessionDto result = serviceTradeSession.selectCard(session.getId(), BOB_KEYCLOAK_ID, card.getId());

            assertNotNull(result.receiverCard());
            assertEquals(card.getId(), result.receiverCard().id());
        }

        @Test
        @DisplayName("selecting a card resets BOTH locks and reverts LOCKED status to ACTIVE")
        void testSelectCard_resetsBothLocks() {
            mockUserLookups();
            TradeSession session = buildSession(TradeSessionStatus.LOCKED);
            session.setInitiatorLocked(true);
            session.setReceiverLocked(true);
            Card card = cards.getFirst();
            giveCardToUser(alice, card);

            when(repositoryTradeSession.findById(session.getId())).thenReturn(Optional.of(session));
            when(repositoryCard.findById(card.getId())).thenReturn(Optional.of(card));
            when(repositoryTradeSession.isCardInActiveSessionForUser(alice, card.getId())).thenReturn(false);
            when(repositoryTradeSession.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TradeSessionDto result = serviceTradeSession.selectCard(session.getId(), ALICE_KEYCLOAK_ID, card.getId());

            assertFalse(session.isInitiatorLocked());
            assertFalse(session.isReceiverLocked());
            assertEquals(TradeSessionStatus.ACTIVE, result.status());
        }

        @Test
        @DisplayName("throws when user does not own the card")
        void testSelectCard_notOwned() {
            mockUserLookups();
            TradeSession session = buildSession(TradeSessionStatus.ACTIVE);
            Card card = cards.getFirst();
            alice.setCardsOwned(new ArrayList<>());

            when(repositoryTradeSession.findById(session.getId())).thenReturn(Optional.of(session));
            when(repositoryCard.findById(card.getId())).thenReturn(Optional.of(card));

            assertThrows(IllegalArgumentException.class,
                    () -> serviceTradeSession.selectCard(session.getId(), ALICE_KEYCLOAK_ID, card.getId()));
        }

        @Test
        @DisplayName("throws when session is not ACTIVE")
        void testSelectCard_notActive() {
            mockUserLookups();
            TradeSession session = buildSession(TradeSessionStatus.PENDING);
            when(repositoryTradeSession.findById(session.getId())).thenReturn(Optional.of(session));

            assertThrows(IllegalStateException.class,
                    () -> serviceTradeSession.selectCard(session.getId(), ALICE_KEYCLOAK_ID, 1L));
        }

        @Test
        @DisplayName("throws when non-participant tries to select")
        void testSelectCard_nonParticipant() {
            mockUserLookups();
            TradeSession session = buildSession(TradeSessionStatus.ACTIVE);
            session.setReceiver(alice); // both sides are alice, bob is excluded

            User charlie = new User();
            charlie.setId(3L);
            charlie.setKeycloakId("kc-charlie-003");
            charlie.setUsername("charlie");
            charlie.setCardsOwned(new ArrayList<>());
            when(repositoryUser.findByKeycloakId("kc-charlie-003")).thenReturn(Optional.of(charlie));
            when(repositoryTradeSession.findById(session.getId())).thenReturn(Optional.of(session));

            assertThrows(IllegalArgumentException.class,
                    () -> serviceTradeSession.selectCard(session.getId(), "kc-charlie-003", 1L));
        }
    }


    // ========================================
    //         LOCK CARD TESTS
    // ========================================

    @Nested
    @DisplayName("lockCard")
    class LockCardTests {

        @Test
        @DisplayName("first lock sets status to LOCKED")
        void testLockCard_initiator() {
            mockUserLookups();
            TradeSession session = buildSession(TradeSessionStatus.ACTIVE);
            Card card = cards.getFirst();
            session.setInitiatorCard(card);

            when(repositoryTradeSession.findById(session.getId())).thenReturn(Optional.of(session));
            when(repositoryTradeSession.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TradeSessionDto result = serviceTradeSession.lockCard(session.getId(), ALICE_KEYCLOAK_ID);

            assertTrue(result.initiatorLocked());
            assertFalse(result.receiverLocked());
            assertEquals(TradeSessionStatus.LOCKED, result.status());
        }

        @Test
        @DisplayName("throws when no card selected")
        void testLockCard_noCard() {
            mockUserLookups();
            TradeSession session = buildSession(TradeSessionStatus.ACTIVE);

            when(repositoryTradeSession.findById(session.getId())).thenReturn(Optional.of(session));

            assertThrows(IllegalStateException.class,
                    () -> serviceTradeSession.lockCard(session.getId(), ALICE_KEYCLOAK_ID));
        }

        @Test
        @DisplayName("both lock with same rarity triggers trade completion")
        void testLockCard_bothLock_executeTrade() {
            mockUserLookups();
            TradeSession session = buildSession(TradeSessionStatus.ACTIVE);

            Card aliceCard = cards.stream().filter(c -> c.getRarity() == Rarity.COMMON).findFirst().orElseThrow();
            Card bobCard = cards.stream().filter(c -> c.getRarity() == Rarity.COMMON).skip(1).findFirst().orElseThrow();

            giveCardToUser(alice, aliceCard);
            giveCardToUser(bob, bobCard);

            session.setInitiatorCard(aliceCard);
            session.setReceiverCard(bobCard);
            session.setInitiatorLocked(true); // alice already locked

            when(repositoryTradeSession.findById(session.getId())).thenReturn(Optional.of(session));
            when(repositoryTradeSession.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(repositoryUser.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TradeSessionDto result = serviceTradeSession.lockCard(session.getId(), BOB_KEYCLOAK_ID);

            assertEquals(TradeSessionStatus.COMPLETED, result.status());
            assertTrue(alice.getCardsOwned().contains(bobCard));
            assertTrue(bob.getCardsOwned().contains(aliceCard));
        }

        @Test
        @DisplayName("second lock from LOCKED status triggers trade")
        void testLockCard_secondLockFromLockedStatus() {
            mockUserLookups();
            TradeSession session = buildSession(TradeSessionStatus.LOCKED);

            Card aliceCard = cards.stream().filter(c -> c.getRarity() == Rarity.COMMON).findFirst().orElseThrow();
            Card bobCard = cards.stream().filter(c -> c.getRarity() == Rarity.COMMON).skip(1).findFirst().orElseThrow();

            giveCardToUser(alice, aliceCard);
            giveCardToUser(bob, bobCard);

            session.setInitiatorCard(aliceCard);
            session.setReceiverCard(bobCard);
            session.setInitiatorLocked(true);

            when(repositoryTradeSession.findById(session.getId())).thenReturn(Optional.of(session));
            when(repositoryTradeSession.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(repositoryUser.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TradeSessionDto result = serviceTradeSession.lockCard(session.getId(), BOB_KEYCLOAK_ID);

            assertEquals(TradeSessionStatus.COMPLETED, result.status());
        }

        @Test
        @DisplayName("both lock with different rarity throws")
        void testLockCard_differentRarity() {
            mockUserLookups();
            TradeSession session = buildSession(TradeSessionStatus.ACTIVE);

            Card commonCard = cards.stream().filter(c -> c.getRarity() == Rarity.COMMON).findFirst().orElseThrow();
            Card rareCard = cards.stream().filter(c -> c.getRarity() == Rarity.RARE).findFirst().orElseThrow();

            giveCardToUser(alice, commonCard);
            giveCardToUser(bob, rareCard);

            session.setInitiatorCard(commonCard);
            session.setReceiverCard(rareCard);
            session.setInitiatorLocked(true);

            when(repositoryTradeSession.findById(session.getId())).thenReturn(Optional.of(session));
            when(repositoryTradeSession.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThrows(IllegalStateException.class,
                    () -> serviceTradeSession.lockCard(session.getId(), BOB_KEYCLOAK_ID));
        }
    }


    // ========================================
    //         CANCEL SESSION TESTS
    // ========================================

    @Nested
    @DisplayName("cancelSession")
    class CancelSessionTests {

        @Test
        @DisplayName("cancels an active session")
        void testCancelSession_success() {
            mockUserLookups();
            TradeSession session = buildSession(TradeSessionStatus.ACTIVE);

            when(repositoryTradeSession.findById(session.getId())).thenReturn(Optional.of(session));
            when(repositoryTradeSession.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TradeSessionDto result = serviceTradeSession.cancelSession(session.getId(), ALICE_KEYCLOAK_ID);

            assertEquals(TradeSessionStatus.CANCELLED, result.status());
            verify(messagingTemplate).convertAndSend(eq("/topic/trade/" + session.getId()), any(TradeSessionDto.class));
        }

        @Test
        @DisplayName("cancels a pending session")
        void testCancelSession_pending() {
            mockUserLookups();
            TradeSession session = buildSession(TradeSessionStatus.PENDING);

            when(repositoryTradeSession.findById(session.getId())).thenReturn(Optional.of(session));
            when(repositoryTradeSession.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TradeSessionDto result = serviceTradeSession.cancelSession(session.getId(), ALICE_KEYCLOAK_ID);

            assertEquals(TradeSessionStatus.CANCELLED, result.status());
        }

        @Test
        @DisplayName("cancels a LOCKED session")
        void testCancelSession_locked() {
            mockUserLookups();
            TradeSession session = buildSession(TradeSessionStatus.LOCKED);

            when(repositoryTradeSession.findById(session.getId())).thenReturn(Optional.of(session));
            when(repositoryTradeSession.save(any())).thenAnswer(inv -> inv.getArgument(0));

            TradeSessionDto result = serviceTradeSession.cancelSession(session.getId(), ALICE_KEYCLOAK_ID);

            assertEquals(TradeSessionStatus.CANCELLED, result.status());
        }

        @Test
        @DisplayName("throws when session is already completed")
        void testCancelSession_alreadyCompleted() {
            mockUserLookups();
            TradeSession session = buildSession(TradeSessionStatus.COMPLETED);

            when(repositoryTradeSession.findById(session.getId())).thenReturn(Optional.of(session));

            assertThrows(IllegalStateException.class,
                    () -> serviceTradeSession.cancelSession(session.getId(), ALICE_KEYCLOAK_ID));
        }

        @Test
        @DisplayName("throws when session is already cancelled")
        void testCancelSession_alreadyCancelled() {
            mockUserLookups();
            TradeSession session = buildSession(TradeSessionStatus.CANCELLED);

            when(repositoryTradeSession.findById(session.getId())).thenReturn(Optional.of(session));

            assertThrows(IllegalStateException.class,
                    () -> serviceTradeSession.cancelSession(session.getId(), ALICE_KEYCLOAK_ID));
        }
    }


    // ========================================
    //         GET STATE TESTS
    // ========================================

    @Nested
    @DisplayName("getState")
    class GetStateTests {

        @Test
        @DisplayName("returns session state for participant")
        void testGetState_success() {
            mockUserLookups();
            TradeSession session = buildSession(TradeSessionStatus.ACTIVE);

            when(repositoryTradeSession.findById(session.getId())).thenReturn(Optional.of(session));

            TradeSessionDto result = serviceTradeSession.getState(session.getId(), ALICE_KEYCLOAK_ID);

            assertNotNull(result);
            assertEquals(TradeSessionStatus.ACTIVE, result.status());
        }

        @Test
        @DisplayName("throws when session not found")
        void testGetState_notFound() {
            mockUserLookups();
            UUID unknownId = UUID.randomUUID();
            when(repositoryTradeSession.findById(unknownId)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class,
                    () -> serviceTradeSession.getState(unknownId, ALICE_KEYCLOAK_ID));
        }
    }


    // ========================================
    //         GET ACTIVE SESSION TESTS
    // ========================================

    @Nested
    @DisplayName("getActiveSession")
    class GetActiveSessionTests {

        @Test
        @DisplayName("returns active session when exists")
        void testGetActiveSession_found() {
            mockUserLookups();
            TradeSession session = buildSession(TradeSessionStatus.ACTIVE);

            when(repositoryTradeSession.findActiveSessionForUser(alice)).thenReturn(Optional.of(session));

            TradeSessionDto result = serviceTradeSession.getActiveSession(ALICE_KEYCLOAK_ID);

            assertNotNull(result);
            assertEquals(TradeSessionStatus.ACTIVE, result.status());
        }

        @Test
        @DisplayName("returns null when no active session")
        void testGetActiveSession_none() {
            mockUserLookups();
            when(repositoryTradeSession.findActiveSessionForUser(alice)).thenReturn(Optional.empty());

            TradeSessionDto result = serviceTradeSession.getActiveSession(ALICE_KEYCLOAK_ID);

            assertNull(result);
        }
    }
}
