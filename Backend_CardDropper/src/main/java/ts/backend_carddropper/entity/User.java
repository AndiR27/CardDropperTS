package ts.backend_carddropper.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ts_user")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "keycloak_id", nullable = false, unique = true)
    private String keycloakId;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    //Liste des cartes possédées par l'utilisateur (via UserCard — une ligne par (user, carte) avec quantité)
    @OneToMany(mappedBy = "user")
    private List<UserCard> userCards = new ArrayList<>();

    //Liste des cartes créées par l'utilisateur
    @OneToMany(mappedBy = "createdBy")
    private List<Card> cardsCreated;

    //Liste des cartes qui ciblent cet utilisateur
    @OneToMany(mappedBy = "targetUser")
    private List<Card> cardsTargeting;
}
