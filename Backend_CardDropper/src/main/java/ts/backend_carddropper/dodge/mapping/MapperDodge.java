package ts.backend_carddropper.dodge.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ts.backend_carddropper.dodge.entity.DodgeScore;
import ts.backend_carddropper.dodge.models.DodgeScoreDto;
import ts.backend_carddropper.mapping.CentralConfig;

@Mapper(config = CentralConfig.class)
public interface MapperDodge {

    @Mapping(target = "username", source = "user.username")
    DodgeScoreDto toDto(DodgeScore dodgeScore);
}
