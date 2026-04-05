package ts.backend_carddropper.reroll.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ts.backend_carddropper.enums.Rarity;

import java.time.LocalDateTime;

@Entity
@Table(name = "reroll_cooldown",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_reroll_id", "rarity"}))
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class RerollCooldown {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_reroll_id", nullable = false)
    private UserReroll userReroll;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rarity rarity;

    // Dernière utilisation du reroll pour cette rareté
    @Column(name = "last_reroll_at", nullable = false)
    private LocalDateTime lastRerollAt;

    public RerollCooldown(UserReroll userReroll, Rarity rarity, LocalDateTime lastRerollAt) {
        this.userReroll = userReroll;
        this.rarity = rarity;
        this.lastRerollAt = lastRerollAt;
    }
}
