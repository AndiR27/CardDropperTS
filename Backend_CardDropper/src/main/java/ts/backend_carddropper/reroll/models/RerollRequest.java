package ts.backend_carddropper.reroll.models;

import jakarta.validation.constraints.NotNull;

public record RerollRequest(
        @NotNull Long cardId
) {}
