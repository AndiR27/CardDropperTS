package ts.backend_carddropper.dodge.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ts.backend_carddropper.entity.User;

import java.time.LocalDate;

@Entity
@Table(name = "dodge_score",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "week_start"}))
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class DodgeScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "best_score", nullable = false)
    private int bestScore;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    public DodgeScore(User user, int bestScore, LocalDate weekStart) {
        this.user = user;
        this.bestScore = bestScore;
        this.weekStart = weekStart;
    }
}
