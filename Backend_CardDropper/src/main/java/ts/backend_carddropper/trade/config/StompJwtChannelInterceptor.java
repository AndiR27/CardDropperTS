package ts.backend_carddropper.trade.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import ts.backend_carddropper.security.KeycloakJwtConverter;

/**
 * L'interceptor est le point d'entrée pour authentifier les connexions WebSocket STOMP.
 * Quand un client ce connecte, il vérifie son badge (le JWT) et si valide, il authentifie l'utilisateur
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final KeycloakJwtConverter keycloakJwtConverter;

    /**
     * La méthode preSend est appelée avant que le message ne soit envoyé au channel.
     * Ici, on vérifie si le message est une commande CONNECT (c'est à dire une tentative de connexion
     * d'un client WebSocket).
     * Si c'est le cas, on extrait le header "Authorization" qui doit contenir le JWT (sous la forme "Bearer
     * token"). On valide le JWT, et si il est valide, on convertit le JWT en un objet d'authentification Spring Security
     * et on l'associe au contexte de sécurité du message (en le mettant dans les headers).
     * yeSi le JWT est invalide ou manquant, on rejette la connexion en lançant une exception.
     * @return
     */
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    Jwt jwt = jwtDecoder.decode(token);
                    JwtAuthenticationToken authentication =
                            (JwtAuthenticationToken) keycloakJwtConverter.convert(jwt);
                    accessor.setUser(authentication);
                    log.debug("WebSocket CONNECT authenticated for user: {}", jwt.getSubject());
                } catch (Exception e) {
                    log.warn("WebSocket CONNECT authentication failed: {}", e.getMessage());
                    throw new IllegalArgumentException("Invalid or expired JWT token");
                }
            } else {
                throw new IllegalArgumentException("Missing Authorization header on CONNECT");
            }
        }

        return message;
    }
}
