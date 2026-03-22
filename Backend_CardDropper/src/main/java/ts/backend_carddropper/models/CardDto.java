package ts.backend_carddropper.models;

import ts.backend_carddropper.enums.Rarity;

public record CardDto(
        Long id,
        String name,
        String imageUrl,
        Rarity rarity,
        String description,
        double dropRate,
        boolean uniqueCard,
        boolean active,
        Long createdById,
        Long targetUserId
) {
}
