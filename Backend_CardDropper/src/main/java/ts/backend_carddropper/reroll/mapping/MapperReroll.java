package ts.backend_carddropper.reroll.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ts.backend_carddropper.mapping.CentralConfig;
import ts.backend_carddropper.reroll.entity.RerollCooldown;
import ts.backend_carddropper.reroll.models.RerollCooldownDto;

import java.time.LocalDateTime;

@Mapper(config = CentralConfig.class)
public interface MapperReroll {

    @Mapping(target = "availableAt", expression = "java(computeAvailableAt(cooldown))")
    RerollCooldownDto toDto(RerollCooldown cooldown);

    default LocalDateTime computeAvailableAt(RerollCooldown cooldown) {
        return cooldown.getLastRerollAt().plus(
                ts.backend_carddropper.reroll.service.ServiceReroll.getCooldownDuration(cooldown.getRarity())
        );
    }
}
