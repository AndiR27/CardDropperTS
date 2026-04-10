package ts.backend_carddropper.minigames.dodge.models;

import java.time.LocalDate;

public record DodgeScoreDto(
        Long id,
        int bestScore,
        String username,
        LocalDate weekStart
) {
}
