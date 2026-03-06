package ts.backend_carddropper.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pack_template_slot")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PackTemplateSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    @JoinColumn(name = "pack_template_id", nullable = false)
    private PackTemplate packTemplate;

    @ManyToOne
    @JoinColumn(name = "pack_slot_id", nullable = false)
    private PackSlot packSlot;

    @Column(nullable = false)
    private int count = 1;
}
