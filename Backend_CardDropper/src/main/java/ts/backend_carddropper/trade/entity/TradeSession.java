package ts.backend_carddropper.trade.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;
import ts.backend_carddropper.entity.Card;
import ts.backend_carddropper.entity.User;
import ts.backend_carddropper.trade.enums.TradeSessionStatus;

import java.time.LocalDate;

@Entity
@Table(name = "trade_session")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TradeSession {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "session_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TradeSessionStatus status;

    @ManyToOne
    @JoinColumn(name = "user1_id", nullable = false)
    private User initiator;

    @ManyToOne
    @JoinColumn(name = "user2_id", nullable = false)
    private User receiver;

    @ManyToOne
    @JoinColumn(name = "card1_id", nullable = false)
    private Card initiatorCard;

    @ManyToOne
    @JoinColumn(name = "card2_id", nullable = false)
    private Card receiverCard;

    @Column(name = "initiator_locked", nullable = false)
    private boolean initiatorLocked;

    @Column(name = "receiver_locked", nullable = false)
    private boolean receiverLocked;

    @Column(name = "created_at", nullable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate createdAt;

    @Column(name = "completed_at", nullable = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate completedAt;



}
