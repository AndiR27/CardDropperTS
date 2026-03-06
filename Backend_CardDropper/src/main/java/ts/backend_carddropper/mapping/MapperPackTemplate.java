package ts.backend_carddropper.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ts.backend_carddropper.entity.PackTemplate;
import ts.backend_carddropper.entity.PackTemplateSlot;
import ts.backend_carddropper.models.PackTemplateDto;
import ts.backend_carddropper.models.PackTemplateSlotDto;

@Mapper(config = CentralConfig.class)
public interface MapperPackTemplate {

    PackTemplateDto toDto(PackTemplate entity);

    @Mapping(target = "slots", ignore = true)
    PackTemplate toEntity(PackTemplateDto dto);

    @Mapping(target = "slots", ignore = true)
    void updateEntity(PackTemplateDto dto, @MappingTarget PackTemplate entity);

    @Mapping(target = "slotId", source = "packSlot.id")
    @Mapping(target = "slotName", source = "packSlot.name")
    PackTemplateSlotDto toSlotDto(PackTemplateSlot entity);
}
