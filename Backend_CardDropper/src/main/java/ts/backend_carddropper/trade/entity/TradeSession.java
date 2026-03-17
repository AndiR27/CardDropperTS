package ts.backend_carddropper.trade.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ts.backend_carddropper.entity.Card;
import ts.backend_carddropper.entity.User;
import ts.backend_carddropper.trade.enums.TradeSessionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "trade_session")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TradeSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "session_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TradeSessionStatus status;

    @ManyToOne
    @JoinColumn(name = "initiator_id", nullable = false)
    private User initiator;

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @ManyToOne
    @JoinColumn(name = "initiator_card_id")
    private Card initiatorCard;

    @ManyToOne
    @JoinColumn(name = "receiver_card_id")
    private Card receiverCard;

    @Column(name = "initiator_locked", nullable = false)
    private boolean initiatorLocked;

    @Column(name = "receiver_locked", nullable = false)
    private boolean receiverLocked;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}