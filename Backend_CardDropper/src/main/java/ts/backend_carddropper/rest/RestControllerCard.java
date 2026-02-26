package ts.backend_carddropper.rest;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ts.backend_carddropper.api.CardApi;
import ts.backend_carddropper.enums.Rarity;
import ts.backend_carddropper.models.CardDto;
import ts.backend_carddropper.service.ServiceCard;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RestControllerCard implements CardApi {

    private final ServiceCard serviceCard;


    //==============================
    //    CRUD
    //==============================

    @Override
    public ResponseEntity<List<CardDto>> getCards() {
        return ResponseEntity.ok(serviceCard.findAll());
    }

    @Override
    public ResponseEntity<CardDto> getCardById(Long cardId) {
        return serviceCard.findById(cardId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<List<CardDto>> getCardsByRarity(Rarity rarity) {
        return ResponseEntity.ok(serviceCard.findByRarity(rarity));
    }

    @Override
    public ResponseEntity<CardDto> createCard(CardDto cardDto) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(serviceCard.createCard(cardDto));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Override
    public ResponseEntity<CardDto> updateCard(Long cardId, CardDto cardDto) {
        return serviceCard.update(cardId, cardDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Void> deleteCard(Long cardId) {
        try {
            serviceCard.delete(cardId);
            return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }


    //==============================
    //    TRANSFER
    //==============================

    @Override
    public ResponseEntity<CardDto> transferCardOwnership(Long cardId, Long newOwnerId) {
        try {
            return ResponseEntity.ok(serviceCard.transferOwnership(cardId, newOwnerId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
