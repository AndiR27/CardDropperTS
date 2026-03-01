package ts.backend_carddropper.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
                    newUser.setCardsOwned(new ArrayList<>());
                    newUser.setCardsCreated(new ArrayList<>());
                    newUser.setCardsTargeting(new ArrayList<>());
                    User saved = repositoryUser.save(newUser);
                    log.info("Created local user '{}' for keycloakId={}", saved.getUsername(), keycloakId);
                    return saved;
                });

        return mapperUser.toDto(user);
    }
}