package ts.backend_carddropper.trade.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import ts.backend_carddropper.entity.Card;
import ts.backend_carddropper.entity.User;
import ts.backend_carddropper.repository.RepositoryCard;
import ts.backend_carddropper.repository.RepositoryUser;
import ts.backend_carddropper.trade.entity.TradeSession;
import ts.backend_carddropper.trade.enums.TradeSessionStatus;
import ts.backend_carddropper.trade.mapping.MapperTradeSession;
import ts.backend_carddropper.trade.models.TradeSessionDto;
import ts.backend_carddropper.trade.repository.RepositoryTradeSession;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServiceTradeSession {

    private final RepositoryTradeSession repositoryTradeSession;
    private final RepositoryUser repositoryUser;
    private final RepositoryCard repositoryCard;
    private final MapperTradeSession mapperTradeSession;
    private final SimpMessagingTemplate messagingTemplate;

    // ==============================
    //    CREATE SESSION
    // ==============================

    @Transactional
    public TradeSessionDto createSession(String keycloakId, Long receiverId) {
        User initiator = findUserByKeycloakId(keycloakId);
        User receiver = repositoryUser.findById(receiverId)
                .orElseThrow(() -> new EntityNotFoundException("Receiver not found with id: " + receiverId));

        if (initiator.getId().equals(receiver.getId())) {
            throw new IllegalArgumentException("Cannot trade with yourself");
        }

        // Check neither user has an active session
        repositoryTradeSession.findActiveSessionForUser(initiator).ifPresent(s -> {
            throw new IllegalStateException("Initiator already has an active trade session");
        });
        repositoryTradeSession.findActiveSessionForUser(receiver).ifPresent(s -> {
            throw new IllegalStateException("Receiver already has an active trade session");
        });

        TradeSession session = buildNewSession(initiator, receiver);
        session = repositoryTradeSession.save(session);
        log.info("Trade session {} created: {} -> {}", session.getId(), initiator.getUsername(), receiver.getUsername());

        TradeSessionDto dto = mapperTradeSession.toDto(session);

        // Notify the receiver that they have a pending trade invite
        // Principal.getName() returns the username (set by KeycloakJwtConverter)
        messagingTemplate.convertAndSendToUser(
                receiver.getUsername(), "/queue/trade-invites", dto);

        return dto;
    }

    // ==============================
    //    JOIN SESSION
    // ==============================

    @Transactional
    public TradeSessionDto joinSession(UUID sessionId, String keycloakId) {
        TradeSession session = findSessionOrThrow(sessionId);
        User user = findUserByKeycloakId(keycloakId);

        if (!session.getReceiver().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Only the invited receiver can join this session");
        }
        if (session.getStatus() != TradeSessionStatus.PENDING) {
            throw new IllegalStateException("Session is not in PENDING status");
        }

        session.setStatus(TradeSessionStatus.ACTIVE);
        session = repositoryTradeSession.save(session);
        log.info("Trade session {} joined by {}", sessionId, user.getUsername());

        TradeSessionDto dto = mapperTradeSession.toDto(session);
        broadcast(session.getId(), dto);
        return dto;
    }

    // ==============================
    //    SELECT CARD
    // ==============================

    @Transactional
    public TradeSessionDto selectCard(UUID sessionId, String keycloakId, Long cardId) {
        TradeSession session = findSessionOrThrow(sessionId);
        User user = findUserByKeycloakId(keycloakId);
        validateParticipant(session, user);

        if (session.getStatus() != TradeSessionStatus.ACTIVE && session.getStatus() != TradeSessionStatus.LOCKED) {
            throw new IllegalStateException("Session is not ACTIVE or LOCKED");
        }

        Card card = repositoryCard.findById(cardId)
                .orElseThrow(() -> new EntityNotFoundException("Card not found with id: " + cardId));

        // Verify user owns the card
        boolean owns = user.getCardsOwned().stream().anyMatch(c -> c.getId().equals(cardId));
        if (!owns) {
            throw new IllegalArgumentException("User does not own card id=" + cardId);
        }

        // Check this user doesn't already have this card committed in another active session
        if (repositoryTradeSession.isCardInActiveSessionForUser(user, cardId)) {
            // Allow re-selecting the same card in the current session
            boolean isCurrentSelection = isInitiatorSide(session, user)
                    ? session.getInitiatorCard() != null && session.getInitiatorCard().getId().equals(cardId)
                    : session.getReceiverCard() != null && session.getReceiverCard().getId().equals(cardId);
            if (!isCurrentSelection) {
                throw new IllegalStateException("You already have this card in another active trade session");
            }
        }

        // Set card on correct side and reset BOTH locks (anti-scam)
        if (isInitiatorSide(session, user)) {
            session.setInitiatorCard(card);
            session.setInitiatorLocked(false);
            session.setReceiverLocked(false);
        } else {
            session.setReceiverCard(card);
            session.setReceiverLocked(false);
            session.setInitiatorLocked(false);
        }

        // Always revert to ACTIVE since locks were reset
        session.setStatus(TradeSessionStatus.ACTIVE);

        session = repositoryTradeSession.save(session);
        log.info("Trade session {}: {} selected card {}", sessionId, user.getUsername(), cardId);

        TradeSessionDto dto = mapperTradeSession.toDto(session);
        broadcast(session.getId(), dto);
        return dto;
    }

    // ==============================
    //    LOCK CARD
    // ==============================

    @Transactional
    public TradeSessionDto lockCard(UUID sessionId, String keycloakId) {
        TradeSession session = findSessionOrThrow(sessionId);
        User user = findUserByKeycloakId(keycloakId);
        validateParticipant(session, user);

        if (session.getStatus() != TradeSessionStatus.ACTIVE && session.getStatus() != TradeSessionStatus.LOCKED) {
            throw new IllegalStateException("Session is not ACTIVE or LOCKED");
        }

        if (isInitiatorSide(session, user)) {
            if (session.getInitiatorCard() == null) {
                throw new IllegalStateException("No card selected to lock");
            }
            session.setInitiatorLocked(true);
        } else {
            if (session.getReceiverCard() == null) {
                throw new IllegalStateException("No card selected to lock");
            }
            session.setReceiverLocked(true);
        }

        // If both locked, validate same rarity and execute
        if (session.isInitiatorLocked() && session.isReceiverLocked()) {
            if (session.getInitiatorCard().getRarity() != session.getReceiverCard().getRarity()) {
                throw new IllegalStateException("Cards must be of the same rarity to trade");
            }
            session = repositoryTradeSession.save(session);
            return executeTrade(session);
        }

        // First player locked — set status to LOCKED
        session.setStatus(TradeSessionStatus.LOCKED);
        session = repositoryTradeSession.save(session);
        log.info("Trade session {}: {} locked their card", sessionId, user.getUsername());

        TradeSessionDto dto = mapperTradeSession.toDto(session);
        broadcast(session.getId(), dto);
        return dto;
    }

    // ==============================
    //    CANCEL SESSION
    // ==============================

    @Transactional
    public TradeSessionDto cancelSession(UUID sessionId, String keycloakId) {
        TradeSession session = findSessionOrThrow(sessionId);
        User user = findUserByKeycloakId(keycloakId);
        validateParticipant(session, user);

        if (session.getStatus() == TradeSessionStatus.COMPLETED || session.getStatus() == TradeSessionStatus.CANCELLED) {
            throw new IllegalStateException("Session is already " + session.getStatus());
        }

        session.setStatus(TradeSessionStatus.CANCELLED);
        session.setCompletedAt(LocalDateTime.now());
        session = repositoryTradeSession.save(session);
        log.info("Trade session {} cancelled by {}", sessionId, user.getUsername());

        TradeSessionDto dto = mapperTradeSession.toDto(session);
        broadcast(session.getId(), dto);
        return dto;
    }

    // ==============================
    //    GET STATE
    // ==============================

    public TradeSessionDto getState(UUID sessionId, String keycloakId) {
        TradeSession session = findSessionOrThrow(sessionId);
        User user = findUserByKeycloakId(keycloakId);
        validateParticipant(session, user);
        return mapperTradeSession.toDto(session);
    }

    // ==============================
    //    GET ACTIVE SESSION
    // ==============================

    public TradeSessionDto getActiveSession(String keycloakId) {
        User user = findUserByKeycloakId(keycloakId);
        return repositoryTradeSession.findActiveSessionForUser(user)
                .map(mapperTradeSession::toDto)
                .orElse(null);
    }

    // ==============================
    //    PRIVATE HELPERS
    // ==============================

    private TradeSession buildNewSession(User initiator, User receiver) {
        TradeSession session = new TradeSession();
        session.setStatus(TradeSessionStatus.PENDING);
        session.setInitiator(initiator);
        session.setReceiver(receiver);
        session.setInitiatorLocked(false);
        session.setReceiverLocked(false);
        session.setCreatedAt(LocalDateTime.now());
        return session;
    }

    private TradeSession findSessionOrThrow(UUID sessionId) {
        return repositoryTradeSession.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Trade session not found: " + sessionId));
    }

    private User findUserByKeycloakId(String keycloakId) {
        return repositoryUser.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new EntityNotFoundException("User not found for keycloakId: " + keycloakId));
    }

    private void validateParticipant(TradeSession session, User user) {
        boolean isInitiator = session.getInitiator().getId().equals(user.getId());
        boolean isReceiver = session.getReceiver() != null && session.getReceiver().getId().equals(user.getId());
        if (!isInitiator && !isReceiver) {
            throw new IllegalArgumentException("User is not a participant of this trade session");
        }
    }

    private boolean isInitiatorSide(TradeSession session, User user) {
        return session.getInitiator().getId().equals(user.getId());
    }

    private void broadcast(UUID sessionId, TradeSessionDto dto) {
        messagingTemplate.convertAndSend("/topic/trade/" + sessionId, dto);
    }

    private TradeSessionDto executeTrade(TradeSession session) {
        User initiator = session.getInitiator();
        User receiver = session.getReceiver();
        Card initiatorCard = session.getInitiatorCard();
        Card receiverCard = session.getReceiverCard();

        // Verify ownership still valid
        boolean initiatorOwns = initiator.getCardsOwned().stream()
                .anyMatch(c -> c.getId().equals(initiatorCard.getId()));
        boolean receiverOwns = receiver.getCardsOwned().stream()
                .anyMatch(c -> c.getId().equals(receiverCard.getId()));

        if (!initiatorOwns || !receiverOwns) {
            throw new IllegalStateException("One or both users no longer own their selected cards");
        }

        // Swap cards
        initiator.getCardsOwned().remove(initiatorCard);
        receiver.getCardsOwned().add(initiatorCard);

        receiver.getCardsOwned().remove(receiverCard);
        initiator.getCardsOwned().add(receiverCard);

        repositoryUser.save(initiator);
        repositoryUser.save(receiver);

        session.setStatus(TradeSessionStatus.COMPLETED);
        session.setCompletedAt(LocalDateTime.now());
        session = repositoryTradeSession.save(session);

        log.info("Trade session {} completed: {} gave card {} and received card {} from {}",
                session.getId(), initiator.getUsername(), initiatorCard.getId(),
                receiverCard.getId(), receiver.getUsername());

        TradeSessionDto dto = mapperTradeSession.toDto(session);
        broadcast(session.getId(), dto);
        return dto;
    }
}