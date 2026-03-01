package ts.backend_carddropper.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ts.backend_carddropper.entity.PackTemplate;
import ts.backend_carddropper.models.PackTemplateDto;

@Mapper(config = CentralConfig.class, uses = {MapperPackSlot.class})
public interface MapperPackTemplate {

    PackTemplateDto toDto(PackTemplate entity);

    @Mapping(target = "slots", ignore = true)
    PackTemplate toEntity(PackTemplateDto dto);

    @Mapping(target = "slots", ignore = true)
    void updateEntity(PackTemplateDto dto, @MappingTarget PackTemplate entity);
}
