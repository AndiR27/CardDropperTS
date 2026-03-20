package ts.backend_carddropper.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ts.backend_carddropper.entity.Card;
import ts.backend_carddropper.models.CardDto;

@Mapper(config = CentralConfig.class)
public interface MapperCard {

    @Mapping(target = "createdById", source = "createdBy.id")
    @Mapping(target = "targetUserId", source = "targetUser.id")
    CardDto toDto(Card entity);

    @Mapping(target = "userCards", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "targetUser", ignore = true)
    Card toEntity(CardDto dto);

    @Mapping(target = "userCards", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "targetUser", ignore = true)
    void updateEntity(CardDto dto, @MappingTarget Card entity);
}
