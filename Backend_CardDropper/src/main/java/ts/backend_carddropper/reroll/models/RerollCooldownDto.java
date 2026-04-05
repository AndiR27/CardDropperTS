package ts.backend_carddropper.reroll.models;

import ts.backend_carddropper.enums.Rarity;

import java.time.LocalDateTime;

public record RerollCooldownDto(
        Rarity rarity,
        LocalDateTime lastRerollAt,
        LocalDateTime availableAt
) {}
