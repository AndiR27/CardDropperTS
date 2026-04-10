package ts.backend_carddropper.minigames.dodge.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ts.backend_carddropper.minigames.dodge.entity.DodgeScore;
import ts.backend_carddropper.minigames.dodge.models.DodgeScoreDto;
import ts.backend_carddropper.mapping.CentralConfig;

@Mapper(config = CentralConfig.class)
public interface MapperDodge {

    @Mapping(target = "username", source = "user.username")
    DodgeScoreDto toDto(DodgeScore dodgeScore);
}
