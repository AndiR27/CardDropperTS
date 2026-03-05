package ts.backend_carddropper.service;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import ts.backend_carddropper.entity.User;
import ts.backend_carddropper.mapping.MapperUser;
import ts.backend_carddropper.models.UserDto;
import ts.backend_carddropper.repository.RepositoryUser;

import java.util.ArrayList;

/**
 * Dev-only auth service — uses a fixed local user instead of Keycloak JWT.
 * Overrides ServiceAuth when the "dev" profile is active.
 */
@Slf4j
@Service
@Primary
@Profile("dev")
public class DevServiceAuth extends ServiceAuth {

    private static final String DEV_KEYCLOAK_ID = "fake-id";
    private static final String DEV_USERNAME = "dev";
    private static final String DEV_EMAIL = "dev@mail.com";

    private final RepositoryUser repositoryUser;
    private final MapperUser mapperUser;

    public DevServiceAuth(RepositoryUser repositoryUser, MapperUser mapperUser) {
        super(repositoryUser, mapperUser);
        this.repositoryUser = repositoryUser;
        this.mapperUser = mapperUser;
    }

    @Override
    public Long getCurrentUserId() {
        return getOrCreateDevUser().id();
    }

    @Override
    @Transactional
    public UserDto getOrCreateCurrentUser() {
        return getOrCreateDevUser();
    }

    private UserDto getOrCreateDevUser() {
        User user = repositoryUser.findByKeycloakId(DEV_KEYCLOAK_ID)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setKeycloakId(DEV_KEYCLOAK_ID);
                    newUser.setUsername(DEV_USERNAME);
                    newUser.setEmail(DEV_EMAIL);
                    newUser.setCardsOwned(new ArrayList<>());
                    newUser.setCardsCreated(new ArrayList<>());
                    newUser.setCardsTargeting(new ArrayList<>());
                    User saved = repositoryUser.save(newUser);
                    log.info("Created dev user '{}' (keycloakId={})", saved.getUsername(), DEV_KEYCLOAK_ID);
                    return saved;
                });

        return mapperUser.toDto(user);
    }
}