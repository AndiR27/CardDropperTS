package ts.backend_carddropper.trade.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.socket.EnableWebSocketSecurity;
import org.springframework.security.messaging.access.intercept.MessageMatcherDelegatingAuthorizationManager;
import org.springframework.messaging.support.ChannelInterceptor;

@Configuration
@EnableWebSocketSecurity
public class WebSocketSecurityConfig {

    @Bean
    AuthorizationManager<Message<?>> messageAuthorizationManager(
            MessageMatcherDelegatingAuthorizationManager.Builder messages) {
        return messages
                .simpDestMatchers("/topic/trade/**", "/app/trade/**").authenticated()
                .anyMessage().permitAll()
                .build();
    }

    // Disable CSRF for WebSocket — auth is handled by StompJwtChannelInterceptor
    @Bean("csrfChannelInterceptor")
    ChannelInterceptor csrfChannelInterceptor() {
        return new ChannelInterceptor() {};
    }
}
