package ts.backend_carddropper.entity;


import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ts.backend_carddropper.enums.Rarity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "card")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private Long id;

    private String name;

    // Chemin public (ce que le front va charger)
    // ex: "/media/cards/3f2c...-a9.png"
    @Column(name = "image_url", length = 512)
    private String imageUrl;

    // Rareté de la carte (ex: "COMMON", "RARE", "EPIC", "LEGENDARY")
    @Enumerated(EnumType.STRING)
    @Column(name = "rarity", nullable = false)
    private Rarity rarity;

    @Column(name = "description")
    private String description;

    @Column(name = "drop_rate", nullable = false)
    private double dropRate;

    @Column(name = "is_unique", nullable = false)
    private boolean uniqueCard;

    // Propriétaires de la carte (relation ManyToMany gérée côté User)
    @ManyToMany(mappedBy = "cardsOwned")
    private List<User> owners = new ArrayList<>();

    // Créateur de la carte
    @ManyToOne
    @JoinColumn(name = "creator_id")
    private User createdBy;

    //Relation pour une carte ciblée
    @ManyToOne
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    public void addOwner(User user) {
        if (!owners.contains(user)) {
            owners.add(user);
            user.getCardsOwned().add(this);
        }
    }
}
