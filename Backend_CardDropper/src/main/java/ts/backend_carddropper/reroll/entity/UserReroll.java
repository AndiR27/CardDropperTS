package ts.backend_carddropper.reroll.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ts.backend_carddropper.entity.User;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_reroll")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserReroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    // Cooldowns par rareté (un par rareté utilisée)
    @OneToMany(mappedBy = "userReroll", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RerollCooldown> cooldowns = new ArrayList<>();

    public UserReroll(User user) {
        this.user = user;
    }
}
