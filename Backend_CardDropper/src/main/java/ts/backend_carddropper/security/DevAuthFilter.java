package ts.backend_carddropper.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Dev-only filter — injects a fake JWT into the SecurityContext
 * so that SecurityUtils static methods work without Keycloak.
 */
@Component
@Profile("dev")
public class DevAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            Jwt jwt = new Jwt(
                    "dev-token",
                    Instant.now(),
                    Instant.now().plusSeconds(3600),
                    Map.of("alg", "none"),
                    Map.of(
                            "sub", "fake-id",
                            "preferred_username", "dev",
                            "email", "dev@mail.com"
                    )
            );
            JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }
}