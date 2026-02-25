package ts.backend_carddropper.models;

import java.util.List;

public record UserDto(
        Long id,
        String username,
        String email,
        String passwordHash,
        List<CardDto> cardsOwned,
        List<CardDto> cardsCreated,
        List<CardDto> cardsTargeting
) {
}
