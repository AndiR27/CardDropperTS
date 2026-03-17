package ts.backend_carddropper.trade.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ts.backend_carddropper.entity.User;
import ts.backend_carddropper.trade.entity.TradeSession;
import ts.backend_carddropper.trade.enums.TradeSessionStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RepositoryTradeSession extends JpaRepository<TradeSession, UUID> {

    List<TradeSession> findByStatus(TradeSessionStatus status);

    List<TradeSession> findByInitiatorOrReceiver(User initiator, User receiver);

    @Query("SELECT t FROM TradeSession t WHERE (t.initiator = :user OR t.receiver = :user) " +
            "AND t.status IN (ts.backend_carddropper.trade.enums.TradeSessionStatus.PENDING, " +
            "ts.backend_carddropper.trade.enums.TradeSessionStatus.ACTIVE, " +
            "ts.backend_carddropper.trade.enums.TradeSessionStatus.LOCKED)")
    Optional<TradeSession> findActiveSessionForUser(@Param("user") User user);

    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM TradeSession t " +
            "WHERE ((t.initiator = :user AND t.initiatorCard.id = :cardId) " +
            "OR (t.receiver = :user AND t.receiverCard.id = :cardId)) " +
            "AND t.status IN (ts.backend_carddropper.trade.enums.TradeSessionStatus.PENDING, " +
            "ts.backend_carddropper.trade.enums.TradeSessionStatus.ACTIVE, " +
            "ts.backend_carddropper.trade.enums.TradeSessionStatus.LOCKED)")
    boolean isCardInActiveSessionForUser(@Param("user") User user, @Param("cardId") Long cardId);
}