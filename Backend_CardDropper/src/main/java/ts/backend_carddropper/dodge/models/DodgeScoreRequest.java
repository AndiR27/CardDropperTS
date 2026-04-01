package ts.backend_carddropper.dodge.models;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record DodgeScoreRequest(
        @NotBlank String token,
        @Min(0) int score
) {
}
