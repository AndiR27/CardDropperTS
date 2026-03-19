package ts.backend_carddropper.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "live_feed_event")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class LiveFeedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "actor_username", nullable = false)
    private String actorUsername;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "card_name", nullable = false)
    private String cardName;

    @Column(name = "card_rarity", nullable = false)
    private String cardRarity;

    @Column(name = "target_username", nullable = false)
    private String targetUsername;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
