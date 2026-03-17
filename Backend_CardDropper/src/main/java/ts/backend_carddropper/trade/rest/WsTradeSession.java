package ts.backend_carddropper.trade.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Controller;
import ts.backend_carddropper.trade.service.ServiceTradeSession;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 *  Technical: C'est un @Controller Spring qui utilise @MessageMapping au lieu de @GetMapping/@PostMapping. Chaque méthode écoute un "chemin" STOMP (ex: /app/trade/{id}/select)
 *  au lieu d'une URL HTTP. Le Principal injecté automatiquement est celui qui a été authentifié par le StompJwtChannelInterceptor au moment du CONNECT. Le service fait le
 *  travail métier et broadcast le résultat sur /topic/trade/{id}.
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WsTradeSession {

    private final ServiceTradeSession serviceTradeSession;

    @MessageMapping("/trade/{id}/select")
    public void selectCard(@DestinationVariable UUID id, Map<String, Long> body, Principal principal) {
        String keycloakId = extractKeycloakId(principal);
        Long cardId = body.get("cardId");
        serviceTradeSession.selectCard(id, keycloakId, cardId);
    }

    @MessageMapping("/trade/{id}/lock")
    public void lockCard(@DestinationVariable UUID id, Principal principal) {
        String keycloakId = extractKeycloakId(principal);
        serviceTradeSession.lockCard(id, keycloakId);
    }

    @MessageMapping("/trade/{id}/cancel")
    public void cancelSession(@DestinationVariable UUID id, Principal principal) {
        String keycloakId = extractKeycloakId(principal);
        serviceTradeSession.cancelSession(id, keycloakId);
    }

    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public Map<String, String> handleException(Exception ex) {
        log.warn("WebSocket error: {}", ex.getMessage());
        return Map.of("error", ex.getMessage());
    }

    private String extractKeycloakId(Principal principal) {
        JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) principal;
        Jwt jwt = jwtAuth.getToken();
        return jwt.getSubject();
    }
}
