package ts.backend_carddropper.trade.rest;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ts.backend_carddropper.security.SecurityUtils;
import ts.backend_carddropper.trade.models.TradeSessionDto;
import ts.backend_carddropper.trade.models.TradeSessionRequestDto;
import ts.backend_carddropper.trade.service.PresenceService;
import ts.backend_carddropper.trade.service.ServiceTradeSession;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/trades")
@RequiredArgsConstructor
public class RestTradeSession {

    private final ServiceTradeSession serviceTradeSession;
    private final PresenceService presenceService;

    @PostMapping
    public ResponseEntity<TradeSessionDto> createSession(@Valid @RequestBody TradeSessionRequestDto request) {
        String keycloakId = SecurityUtils.getCurrentKeycloakId();
        TradeSessionDto dto = serviceTradeSession.createSession(keycloakId, request.receiverId());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<TradeSessionDto> joinSession(@PathVariable UUID id) {
        String keycloakId = SecurityUtils.getCurrentKeycloakId();
        return ResponseEntity.ok(serviceTradeSession.joinSession(id, keycloakId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TradeSessionDto> getState(@PathVariable UUID id) {
        String keycloakId = SecurityUtils.getCurrentKeycloakId();
        return ResponseEntity.ok(serviceTradeSession.getState(id, keycloakId));
    }

    @GetMapping("/online")
    public ResponseEntity<List<Map<String, Object>>> getOnlineUsers() {
        return ResponseEntity.ok(presenceService.getOnlineUsersList());
    }

    @GetMapping("/active")
    public ResponseEntity<TradeSessionDto> getActiveSession() {
        String keycloakId = SecurityUtils.getCurrentKeycloakId();
        TradeSessionDto dto = serviceTradeSession.getActiveSession(keycloakId);
        if (dto == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(dto);
    }
}