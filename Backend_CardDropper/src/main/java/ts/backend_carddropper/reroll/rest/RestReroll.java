package ts.backend_carddropper.reroll.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ts.backend_carddropper.reroll.models.RerollCooldownDto;
import ts.backend_carddropper.reroll.models.RerollRequest;
import ts.backend_carddropper.reroll.models.RerollResponse;
import ts.backend_carddropper.reroll.service.ServiceReroll;
import ts.backend_carddropper.security.SecurityUtils;

import java.util.List;

@RestController
@RequestMapping("/me/reroll")
@RequiredArgsConstructor
public class RestReroll {

    private final ServiceReroll serviceReroll;

    @PostMapping
    public ResponseEntity<RerollResponse> reroll(@Valid @RequestBody RerollRequest request) {
        String keycloakId = SecurityUtils.getCurrentKeycloakId();
        return ResponseEntity.ok(serviceReroll.reroll(keycloakId, request.cardId()));
    }

    @GetMapping("/cooldowns")
    public ResponseEntity<List<RerollCooldownDto>> getCooldowns() {
        String keycloakId = SecurityUtils.getCurrentKeycloakId();
        return ResponseEntity.ok(serviceReroll.getCooldowns(keycloakId));
    }
}
