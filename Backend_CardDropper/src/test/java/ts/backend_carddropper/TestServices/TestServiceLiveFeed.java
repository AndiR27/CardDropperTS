package ts.backend_carddropper.TestServices;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ts.backend_carddropper.entity.LiveFeedEvent;
import ts.backend_carddropper.event.LegendaryDropEvent;
import ts.backend_carddropper.event.UseCardEvent;
import ts.backend_carddropper.models.LiveFeedEventDto;
import ts.backend_carddropper.repository.RepositoryLiveFeed;
import ts.backend_carddropper.service.ServiceLiveFeed;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class TestServiceLiveFeed {

    @Autowired
    private ServiceLiveFeed serviceLiveFeed;

    @MockitoBean
    private RepositoryLiveFeed repositoryLiveFeed;

    private LiveFeedEvent sampleEvent;

    @BeforeEach
    void setUp() {
        sampleEvent = new LiveFeedEvent();
        sampleEvent.setId(1L);
        sampleEvent.setEventType("USE_CARD");
        sampleEvent.setActorUsername("alice");
        sampleEvent.setCardName("Dragon");
        sampleEvent.setCardRarity("EPIC");
        sampleEvent.setTargetUsername("bob");
        sampleEvent.setCreatedAt(LocalDateTime.now());
    }


    // ========================================
    //         ÉVÉNEMENTS DU JOUR
    // ========================================

    @Nested
    @DisplayName("Récupération des événements du jour")
    class TodayEventsTests {

        @Test
        @DisplayName("retourne la liste des événements du jour")
        void testGetTodayEvents_success() {
            when(repositoryLiveFeed.findByCreatedAtAfterOrderByCreatedAtDesc(any(LocalDateTime.class)))
                    .thenReturn(List.of(sampleEvent));

            List<LiveFeedEventDto> result = serviceLiveFeed.getTodayEvents();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("alice", result.getFirst().actorUsername());
            assertEquals("Dragon", result.getFirst().cardName());
            assertEquals("EPIC", result.getFirst().cardRarity());
            assertEquals("bob", result.getFirst().targetUsername());
            assertNotNull(result.getFirst().createdAt());
        }

        @Test
        @DisplayName("retourne une liste vide quand aucun événement aujourd'hui")
        void testGetTodayEvents_empty() {
            when(repositoryLiveFeed.findByCreatedAtAfterOrderByCreatedAtDesc(any(LocalDateTime.class)))
                    .thenReturn(List.of());

            List<LiveFeedEventDto> result = serviceLiveFeed.getTodayEvents();

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("la requête utilise le début de la journée comme borne inférieure")
        void testGetTodayEvents_queriesFromStartOfDay() {
            when(repositoryLiveFeed.findByCreatedAtAfterOrderByCreatedAtDesc(any(LocalDateTime.class)))
                    .thenReturn(List.of());

            serviceLiveFeed.getTodayEvents();

            // Vérifier que la requête est faite avec le début du jour (00:00)
            verify(repositoryLiveFeed).findByCreatedAtAfterOrderByCreatedAtDesc(
                    LocalDate.now().atStartOfDay());
        }
    }


    // ========================================
    //         ABONNEMENT SSE
    // ========================================

    @Nested
    @DisplayName("Abonnement SSE (subscribe)")
    class SubscribeTests {

        @Test
        @DisplayName("retourne un SseEmitter valide")
        void testSubscribe_returnsEmitter() {
            SseEmitter emitter = serviceLiveFeed.subscribe();

            assertNotNull(emitter);
        }

        @Test
        @DisplayName("plusieurs abonnements retournent des emitters distincts")
        void testSubscribe_multipleEmitters() {
            SseEmitter emitter1 = serviceLiveFeed.subscribe();
            SseEmitter emitter2 = serviceLiveFeed.subscribe();

            assertNotNull(emitter1);
            assertNotNull(emitter2);
            assertNotEquals(emitter1, emitter2);
        }
    }


    // ========================================
    //     TRAITEMENT DE L'ÉVÉNEMENT
    // ========================================

    @Nested
    @DisplayName("Traitement de UseCardEvent")
    class EventListenerTests {

        @Test
        @DisplayName("persiste l'événement en base de données")
        void testOnUseCardEvent_persistsToDb() {
            when(repositoryLiveFeed.save(any(LiveFeedEvent.class))).thenReturn(sampleEvent);

            UseCardEvent event = new UseCardEvent(this, "alice", "Dragon", "EPIC", "bob");
            serviceLiveFeed.onUseCardEvent(event);

            // Vérifier que l'entité est bien sauvegardée
            verify(repositoryLiveFeed).save(any(LiveFeedEvent.class));
        }

        @Test
        @DisplayName("l'entité sauvegardée contient les bonnes valeurs")
        void testOnUseCardEvent_correctValues() {
            when(repositoryLiveFeed.save(any(LiveFeedEvent.class))).thenAnswer(inv -> {
                LiveFeedEvent entity = inv.getArgument(0);
                entity.setId(1L);
                entity.setCreatedAt(LocalDateTime.now());
                return entity;
            });

            UseCardEvent event = new UseCardEvent(this, "alice", "Fireball", "RARE", "bob");
            serviceLiveFeed.onUseCardEvent(event);

            // Capturer l'entité passée au repository
            var captor = org.mockito.ArgumentCaptor.forClass(LiveFeedEvent.class);
            verify(repositoryLiveFeed).save(captor.capture());

            LiveFeedEvent saved = captor.getValue();
            assertEquals("USE_CARD", saved.getEventType());
            assertEquals("alice", saved.getActorUsername());
            assertEquals("Fireball", saved.getCardName());
            assertEquals("RARE", saved.getCardRarity());
            assertEquals("bob", saved.getTargetUsername());
        }

        @Test
        @DisplayName("envoie l'événement aux abonnés SSE connectés")
        void testOnUseCardEvent_pushesToSseSubscribers() {
            when(repositoryLiveFeed.save(any(LiveFeedEvent.class))).thenReturn(sampleEvent);

            // Créer un abonné SSE
            SseEmitter emitter = serviceLiveFeed.subscribe();
            assertNotNull(emitter);

            // Déclencher l'événement
            UseCardEvent event = new UseCardEvent(this, "alice", "Dragon", "EPIC", "bob");

            // L'événement ne doit pas lever d'exception même avec des abonnés
            assertDoesNotThrow(() -> serviceLiveFeed.onUseCardEvent(event));
        }

        @Test
        @DisplayName("persiste un LegendaryDropEvent avec le bon type")
        void testOnLegendaryDropEvent_persistsCorrectly() {
            when(repositoryLiveFeed.save(any(LiveFeedEvent.class))).thenAnswer(inv -> {
                LiveFeedEvent entity = inv.getArgument(0);
                entity.setId(2L);
                entity.setCreatedAt(LocalDateTime.now());
                return entity;
            });

            LegendaryDropEvent event = new LegendaryDropEvent(this, "alice");
            serviceLiveFeed.onLegendaryDropEvent(event);

            var captor = org.mockito.ArgumentCaptor.forClass(LiveFeedEvent.class);
            verify(repositoryLiveFeed).save(captor.capture());

            LiveFeedEvent saved = captor.getValue();
            assertEquals("LEGENDARY_DROP", saved.getEventType());
            assertEquals("alice", saved.getActorUsername());
            assertEquals("", saved.getCardName());
            assertEquals("LEGENDARY", saved.getCardRarity());
            assertEquals("", saved.getTargetUsername());
        }
    }
}
