package ts.backend_carddropper.trade.models;

import jakarta.validation.constraints.NotNull;

public record TradeSessionRequestDto(
        @NotNull
        Long receiverId
) {
}
