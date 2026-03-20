package ts.backend_carddropper.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Join entity between User and Card.
 * One row per (user, card) pair — the quantity field tracks how many copies the user owns.
 */
@Entity
@Table(
    name = "user_cards",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "card_id"})
)
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserCard {

    @Id
    @SequenceGenerator(name = "user_cards_seq", sequenceName = "user_cards_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_cards_seq")
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Column(nullable = false)
    private int quantity;

    public UserCard(User user, Card card, int quantity) {
        this.user = user;
        this.card = card;
        this.quantity = quantity;
    }
}
