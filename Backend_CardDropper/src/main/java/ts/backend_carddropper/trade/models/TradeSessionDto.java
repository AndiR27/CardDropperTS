package ts.backend_carddropper.trade.models;

import ts.backend_carddropper.models.CardDto;
import ts.backend_carddropper.trade.enums.TradeSessionStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record TradeSessionDto(
        UUID id,
        TradeSessionStatus status,
        String initiatorUsername,
        String receiverUsername,
        CardDto initiatorCard,
        CardDto receiverCard,
        boolean initiatorLocked,
        boolean receiverLocked,
        LocalDateTime createdAt
) {
}