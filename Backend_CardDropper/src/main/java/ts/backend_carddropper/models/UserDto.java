package ts.backend_carddropper.models;

import java.util.List;

public record UserDto(
        Long id,
        String keycloakId,
        String username,
        String email,
        List<CardDto> cardsOwned,
        List<CardDto> cardsCreated,
        List<CardDto> cardsTargeting
) {
}
