package ts.backend_carddropper.models;

public record LiveFeedEventDto(
        Long id,
        String actorUsername,
        String cardName,
        String cardRarity,
        String targetUsername,
        String createdAt
) {}
