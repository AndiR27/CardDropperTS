package ts.backend_carddropper.models;

import ts.backend_carddropper.enums.Rarity;

import java.util.Map;

public record PackSlotDto(
        Long id,
        String name,
        Rarity fixedRarity,
        Map<String, Double> rarityWeights
) {
}
