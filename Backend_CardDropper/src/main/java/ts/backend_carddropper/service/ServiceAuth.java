package ts.backend_carddropper.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import ts.backend_carddropper.entity.User;
import ts.backend_carddropper.mapping.MapperUser;
import ts.backend_carddropper.models.UserDto;
import ts.backend_carddropper.repository.RepositoryUser;
import ts.backend_carddropper.security.SecurityUtils;

import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceAuth {

    private final RepositoryUser repositoryUser;
    private final MapperUser mapperUser;

    /**
     * Retourne l'id DB de l'utilisateur authentifié via le JWT
     */
    public Long getCurrentUserId() {
        String keycloakId = SecurityUtils.getCurrentKeycloakId();
        return repositoryUser.findByKeycloakId(keycloakId)
                .map(User::getId)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                        "Current user not found in database"));
    }

    public UserDto getUserByKeycloakId(String keycloakId) {
        return repositoryUser.findByKeycloakId(keycloakId)
                .map(mapperUser::toDto)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(
                        "User with keycloakId=" + keycloakId + " not found in database"));
    }

    /**
     * Récupère ou crée l'utilisateur local à partir du JWT Keycloak
     */
    @Transactional
    public UserDto getOrCreateCurrentUser() {
        String keycloakId = SecurityUtils.getCurrentKeycloakId();

        User user = repositoryUser.findByKeycloakId(keycloakId)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setKeycloakId(keycloakId);
                    newUser.setUsername(SecurityUtils.getCurrentUsername());
                    newUser.setEmail(SecurityUtils.getCurrentEmail());
                    newUser.setCardsCreated(new ArrayList<>());
                    newUser.setCardsTargeting(new ArrayList<>());
                    User saved = repositoryUser.save(newUser);
                    log.info("Created local user '{}' for keycloakId={}", saved.getUsername(), keycloakId);
                    return saved;
                });

        UserDto dto = mapperUser.toDto(user);
        boolean admin = isCurrentUserAdmin();
        return new UserDto(dto.id(), dto.keycloakId(), dto.username(), dto.email(), admin,
                dto.cardsOwned(), dto.cardsCreated(), dto.cardsTargeting());
    }

    // Vérifie si l'utilisateur courant a le rôle ADMIN dans son JWT
    private boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN"));
    }
}