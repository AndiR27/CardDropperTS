package ts.backend_carddropper.trade.models;

import jakarta.validation.constraints.NotNull;

public record CardSelectionRequest(
        @NotNull Long cardId
) {
}
