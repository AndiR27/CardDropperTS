package ts.backend_carddropper.trade.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Mise en place de la config WebSocket permettant de gérer la communication entre le backend et le frontend de façon instantanée
 * -> Permet de faire du push d'information en temps réel (ex: mise à jour de l'état d'une session de trade) sans que le client ait besoin de faire du polling
 * -> Utilisation de STOMP comme protocole de messagerie pour structurer les messages échangés entre le client et le serveur
 *
 */
@Configuration
@EnableWebSocketMessageBroker // Active la gestion des WebSockets et de la messagerie STOMP
@Order(Ordered.HIGHEST_PRECEDENCE + 99) // Assure que cette configuration est chargée après la configuration de sécurité (qui est à HIGHEST_PRECEDENCE + 100)
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    private final StompJwtChannelInterceptor stompJwtChannelInterceptor;

    /**
     * Mets en place le système de routage des messages côté serveur (définit ou et comment les messages
     * seront envoyés aux clients abonnés)
     * @param registry
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        //Tous les clients abonnés à une destination commençant par /topic ou /queue recevront les messages envoyés à ces destinations
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    // Définit le point d'entrée WebSocket que les clients Frontend utiliseront pour se connecter au serveur WebSocket
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Native WebSocket (used by @stomp/stompjs in the frontend)
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins.split(","));
        // SockJS fallback (for older browsers)
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins.split(","))
                .withSockJS();
    }

    //Ajoute un interceptor (sécurité) qui check toutes les connexions entrantes pour valider le JWT et authentifier
    // l'utilisateur avant d'autoriser l'accès aux fonctionnalités WebSocket
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        //Lors de la connexion STOMP, l'interceptor va extraire le JWT du header, le valider, et si valide, authentifier
        // l'utilisateur dans le contexte de sécurité de Spring
        registration.interceptors(stompJwtChannelInterceptor);
    }
}