package ts.backend_carddropper.dodge.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ts.backend_carddropper.dodge.models.DodgeScoreDto;
import ts.backend_carddropper.dodge.models.DodgeScoreRequest;
import ts.backend_carddropper.dodge.models.DodgeStartResponse;
import ts.backend_carddropper.dodge.service.ServiceDodge;
import ts.backend_carddropper.security.SecurityUtils;

import java.util.List;

@RestController
@RequestMapping("/me/dodge")
@RequiredArgsConstructor
public class RestDodge {

    private final ServiceDodge serviceDodge;

    @PostMapping("/start")
    public ResponseEntity<DodgeStartResponse> startGame() {
        String keycloakId = SecurityUtils.getCurrentKeycloakId();
        String token = serviceDodge.startGame(keycloakId);
        return ResponseEntity.ok(new DodgeStartResponse(token));
    }

    @PostMapping("/score")
    public ResponseEntity<DodgeScoreDto> submitScore(@Valid @RequestBody DodgeScoreRequest request) {
        String keycloakId = SecurityUtils.getCurrentKeycloakId();
        return ResponseEntity.ok(serviceDodge.submitScore(keycloakId, request.token(), request.score()));
    }

    @GetMapping("/score")
    public ResponseEntity<DodgeScoreDto> getMyScore() {
        String keycloakId = SecurityUtils.getCurrentKeycloakId();
        return ResponseEntity.ok(serviceDodge.getMyScore(keycloakId));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<List<DodgeScoreDto>> getLeaderboard() {
        return ResponseEntity.ok(serviceDodge.getTopScores());
    }
}
