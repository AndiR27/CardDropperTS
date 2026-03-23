package ts.backend_carddropper.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ts.backend_carddropper.entity.LiveFeedEvent;
import ts.backend_carddropper.event.LegendaryDropEvent;
import ts.backend_carddropper.event.UseCardEvent;
import ts.backend_carddropper.mapping.MapperLiveFeed;
import ts.backend_carddropper.models.LiveFeedEventDto;
import ts.backend_carddropper.repository.RepositoryLiveFeed;

import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceLiveFeed {

    private static final long SSE_TIMEOUT = 5 * 60 * 1000L; // 5 minutes

    private final RepositoryLiveFeed repositoryLiveFeed;
    private final MapperLiveFeed mapperLiveFeed;

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @PreDestroy
    void shutdown() {
        log.info("Fermeture de {} connexion(s) SSE", emitters.size());
        for (SseEmitter emitter : emitters) {
            emitter.complete();
        }
        emitters.clear();
    }

    /**
     * Crée un nouveau SseEmitter et l'enregistre pour les mises à jour en temps réel.
     */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        log.info("Nouvel abonné SSE connecté ({} au total)", emitters.size());
        return emitter;
    }

    /**
     * Écoute les UseCardEvent, persiste en base de données et pousse à tous les abonnés SSE.
     */
    @EventListener
    public void onUseCardEvent(UseCardEvent event) {
        LiveFeedEvent entity = new LiveFeedEvent();
        entity.setEventType("USE_CARD");
        entity.setActorUsername(event.getActorUsername());
        entity.setCardName(event.getCardName());
        entity.setCardRarity(event.getCardRarity());
        entity.setTargetUsername(event.getTargetUsername());

        LiveFeedEvent saved = repositoryLiveFeed.save(entity);
        LiveFeedEventDto dto = mapperLiveFeed.toDto(saved);

        log.info("LiveFeed: {} a utilisé '{}' ({}) sur {}",
                dto.actorUsername(), dto.cardName(), dto.cardRarity(), dto.targetUsername());

        broadcast("use-card", dto);
    }

    @EventListener
    public void onLegendaryDropEvent(LegendaryDropEvent event) {
        LiveFeedEvent entity = new LiveFeedEvent();
        entity.setEventType("LEGENDARY_DROP");
        entity.setActorUsername(event.getActorUsername());
        entity.setCardName("");
        entity.setCardRarity("LEGENDARY");
        entity.setTargetUsername("");

        LiveFeedEvent saved = repositoryLiveFeed.save(entity);
        LiveFeedEventDto dto = mapperLiveFeed.toDto(saved);

        log.info("LiveFeed: {} a obtenu une légendaire !", dto.actorUsername());

        broadcast("legendary-drop", dto);
    }

    // Diffuse un événement à tous les abonnés SSE, en supprimant les connexions déconnectées
    private void broadcast(String eventName, LiveFeedEventDto dto) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(dto));
            } catch (Exception e) {
                log.debug("Suppression d'un emitter SSE déconnecté: {}", e.getMessage());
                emitters.remove(emitter);
            }
        }
    }

    /**
     * Retourne tous les événements du live feed depuis le début de la journée.
     */
    public List<LiveFeedEventDto> getTodayEvents() {
        return mapperLiveFeed.toDtoList(
                repositoryLiveFeed.findByCreatedAtAfterOrderByCreatedAtDesc(
                        LocalDate.now(ZoneId.of("Europe/Zurich")).atStartOfDay()));
    }
}
