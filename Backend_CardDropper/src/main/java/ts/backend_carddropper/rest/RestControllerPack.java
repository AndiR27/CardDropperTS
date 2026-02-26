package ts.backend_carddropper.rest;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ts.backend_carddropper.api.PackApi;
import ts.backend_carddropper.models.CardDto;
import ts.backend_carddropper.service.ServicePack;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RestControllerPack implements PackApi {

    private final ServicePack servicePack;


    //==============================
    //    PACK GENERATION
    //==============================

    @Override
    public ResponseEntity<List<CardDto>> generatePack(Long userId, Long templateId) {
        try {
            return ResponseEntity.ok(servicePack.generatePack(userId, templateId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
