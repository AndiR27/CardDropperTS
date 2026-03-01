package ts.backend_carddropper.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ts.backend_carddropper.api.MeApi;
import ts.backend_carddropper.models.CardDto;
import ts.backend_carddropper.service.ServiceAuth;
import ts.backend_carddropper.service.ServiceCard;
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


    //==============================
    //    CARD QUERIES
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
    //    CARD ACTIONS
    //==============================

    @Override
    public ResponseEntity<CardDto> createMyCard(CardDto cardDto) {
        Long userId = serviceAuth.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceUser.createCard(userId, cardDto));
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
    //    PACK ACTIONS
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
}
