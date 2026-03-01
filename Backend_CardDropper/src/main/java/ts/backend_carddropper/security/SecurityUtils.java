package ts.backend_carddropper.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Jwt getCurrentJwt() {
        return (Jwt) SecurityContextHolder.getContext()
                .getAuthentication()
                .getPrincipal();
    }

    public static String getCurrentKeycloakId() {
        return getCurrentJwt().getSubject();
    }

    public static String getCurrentUsername() {
        return getCurrentJwt().getClaimAsString("preferred_username");
    }

    public static String getCurrentEmail() {
        return getCurrentJwt().getClaimAsString("email");
    }
}