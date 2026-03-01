package ts.backend_carddropper.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ts.backend_carddropper.entity.PackSlot;
import ts.backend_carddropper.enums.Rarity;
import ts.backend_carddropper.models.PackSlotDto;

import java.util.Map;
import java.util.stream.Collectors;

@Mapper(config = CentralConfig.class)
public interface MapperPackSlot {

    PackSlotDto toDto(PackSlot entity);

    @Mapping(target = "packTemplate", ignore = true)
    PackSlot toEntity(PackSlotDto dto);

    @Mapping(target = "packTemplate", ignore = true)
    void updateEntity(PackSlotDto dto, @MappingTarget PackSlot entity);

    default Map<Rarity, Double> mapRarityWeights(Map<String, Double> weights) {
        if (weights == null) return null;
        return weights.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> Rarity.valueOf(e.getKey()),
                        Map.Entry::getValue
                ));
    }

    default Map<String, Double> mapRarityWeightsToString(Map<Rarity, Double> weights) {
        if (weights == null) return null;
        return weights.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey().name(),
                        Map.Entry::getValue
                ));
    }
}
