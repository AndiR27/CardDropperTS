package ts.backend_carddropper.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ts.backend_carddropper.enums.Rarity;

import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "pack_slot")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PackSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pack_template_id", nullable = false)
    private PackTemplate packTemplate;

    // Si non null : rareté fixe pour ce slot (ex: toujours RARE)
    // Si null : la rareté est tirée aléatoirement via rarityWeights
    @Enumerated(EnumType.STRING)
    @Column(name = "fixed_rarity")
    private Rarity fixedRarity;

    // Poids par rareté utilisés si fixedRarity est null
    // ex: { COMMON: 70.0, RARE: 25.0, EPIC: 4.0, LEGENDARY: 1.0 }
    @ElementCollection
    @CollectionTable(name = "pack_slot_rarity_weights", joinColumns = @JoinColumn(name = "pack_slot_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "rarity")
    @Column(name = "weight")
    private Map<Rarity, Double> rarityWeights = new HashMap<>();
}
