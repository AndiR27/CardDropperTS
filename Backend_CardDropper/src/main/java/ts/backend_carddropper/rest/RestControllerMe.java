package ts.backend_carddropper.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import ts.backend_carddropper.api.MeApi;
import ts.backend_carddropper.models.CardDto;
import ts.backend_carddropper.models.LiveFeedEventDto;
import ts.backend_carddropper.models.UserDto;
import ts.backend_carddropper.security.SecurityUtils;
import ts.backend_carddropper.service.ServiceAuth;
import ts.backend_carddropper.service.ServiceCard;
import ts.backend_carddropper.service.ServiceLiveFeed;
import ts.backend_carddropper.service.ServicePack;
import ts.backend_carddropper.service.ServiceUser;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RestControllerMe implements MeApi {

    private final ServiceAuth serviceAuth;
    private final ServiceUser serviceUser;
    private final ServiceCard serviceCard;
    private final ServicePack servicePack;
    private final ServiceLiveFeed serviceLiveFeed;


    //==============================
    //    REQUÊTES CARTES
    //==============================

    @Override
    public ResponseEntity<List<CardDto>> getMyCardsOwned() {
        Long userId = serviceAuth.getCurrentUserId();
        return ResponseEntity.ok(serviceUser.getCardsOwned(userId));
    }

    @Override
    public ResponseEntity<List<CardDto>> getMyCardsCreated() {
        Long userId = serviceAuth.getCurrentUserId();
        return ResponseEntity.ok(serviceCard.findAllByCreatedById(userId));
    }

    @Override
    public ResponseEntity<List<CardDto>> getMyCardsTargeting() {
        Long userId = serviceAuth.getCurrentUserId();
        return ResponseEntity.ok(serviceCard.findAllByTargetUserId(userId));
    }


    //==============================
    //    ACTIONS CARTES
    //==============================

    @Override
    public ResponseEntity<CardDto> createMyCard(CardDto cardDto) {
        // Toute création passe par createMyCardWithImage — cet endpoint n'est plus utilisé
        throw new UnsupportedOperationException("Use POST /me/cards/with-image to create cards");
    }

    @Override
    public ResponseEntity<CardDto> createMyCardWithImage(CardDto card, MultipartFile image) {
        Long userId = serviceAuth.getCurrentUserId();
        String username = SecurityUtils.getCurrentUsername();

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(serviceCard.createCardWithImage(card, image, userId, username));
    }

    @Override
    public ResponseEntity<Void> tradeMyCards(Long myCardId, Long targetUserId, Long theirCardId) {
        Long userId = serviceAuth.getCurrentUserId();
        serviceUser.tradeCard(userId, myCardId, targetUserId, theirCardId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<CardDto> mergeMyCards(List<Long> requestBody) {
        Long userId = serviceAuth.getCurrentUserId();
        return ResponseEntity.ok(serviceUser.mergeCards(userId, requestBody));
    }


    //==============================
    //    ACTIONS PACKS
    //==============================

    @Override
    public ResponseEntity<List<CardDto>> openMyPack(List<Long> requestBody) {
        Long userId = serviceAuth.getCurrentUserId();
        return ResponseEntity.ok(serviceUser.openPack(userId, requestBody));
    }

    @Override
    public ResponseEntity<List<CardDto>> generateMyPack(Long templateId) {
        Long userId = serviceAuth.getCurrentUserId();
        return ResponseEntity.ok(servicePack.generatePack(userId, templateId));
    }


    //==============================
    //    LISTE JOUEURS
    //==============================

    @Override
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(serviceUser.findAll());
    }


    //==============================
    //    UTILISER UNE CARTE
    //==============================

    @Override
    public ResponseEntity<Void> useMyCard(Long cardId, Long targetUserId) {
        Long userId = serviceAuth.getCurrentUserId();
        serviceUser.useCard(userId, cardId, targetUserId);
        return ResponseEntity.noContent().build();
    }


    //==============================
    //    FLUX EN DIRECT
    //==============================

    @GetMapping(value = "/me/live-feed/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLiveFeed() {
        return serviceLiveFeed.subscribe();
    }

    @Override
    public ResponseEntity<List<LiveFeedEventDto>> getTodayLiveFeed() {
        return ResponseEntity.ok(serviceLiveFeed.getTodayEvents());
    }
}
