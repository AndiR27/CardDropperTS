package ts.backend_carddropper.models;

public record LiveFeedEventDto(
        Long id,
        String eventType,
        String actorUsername,
        String cardName,
        String cardRarity,
        String targetUsername,
        String createdAt
) {}
