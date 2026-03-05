package ts.backend_carddropper.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ts.backend_carddropper.entity.LiveFeedEvent;
import ts.backend_carddropper.models.LiveFeedEventDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Mapper(config = CentralConfig.class)
public interface MapperLiveFeed {

    @Mapping(target = "createdAt", expression = "java(mapCreatedAt(entity.getCreatedAt()))")
    LiveFeedEventDto toDto(LiveFeedEvent entity);

    List<LiveFeedEventDto> toDtoList(List<LiveFeedEvent> entities);

    default String mapCreatedAt(LocalDateTime createdAt) {
        if (createdAt == null) return null;
        return createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
}
