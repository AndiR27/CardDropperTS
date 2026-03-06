package ts.backend_carddropper.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardDto> createCard(CardDto cardDto) {
        // Création admin sans image — peu utilisé, gardé pour compatibilité API
        throw new UnsupportedOperationException("Use POST /me/cards/with-image to create cards");
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CardDto> updateCard(Long cardId, CardDto cardDto) {
        return serviceCard.update(cardId, cardDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCard(Long cardId) {
        serviceCard.delete(cardId);
        return ResponseEntity.noContent().build();
    }
}
