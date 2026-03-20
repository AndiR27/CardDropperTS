package ts.backend_carddropper.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ts.backend_carddropper.entity.User;
import ts.backend_carddropper.models.UserDto;

@Mapper(config = CentralConfig.class, uses = {MapperCard.class})
public interface MapperUser {

    // cardsOwned est géré manuellement via ServiceUser.getCardsOwned (RepositoryUserCard)
    // admin is set manually in ServiceAuth, default false here
    @Mapping(target = "admin", constant = "false")
    @Mapping(target = "cardsOwned", ignore = true)
    UserDto toDto(User entity);

    @Mapping(target = "userCards", ignore = true)
    @Mapping(target = "cardsCreated", ignore = true)
    @Mapping(target = "cardsTargeting", ignore = true)
    User toEntity(UserDto dto);

    @Mapping(target = "userCards", ignore = true)
    @Mapping(target = "cardsCreated", ignore = true)
    @Mapping(target = "cardsTargeting", ignore = true)
    void updateEntity(UserDto dto, @MappingTarget User entity);
}
