package ts.backend_carddropper.trade.mapping;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ts.backend_carddropper.mapping.CentralConfig;
import ts.backend_carddropper.mapping.MapperCard;
import ts.backend_carddropper.trade.entity.TradeSession;
import ts.backend_carddropper.trade.models.TradeSessionDto;

@Mapper(config = CentralConfig.class, uses = MapperCard.class)
public interface MapperTradeSession {

    @Mapping(target = "initiatorUsername", source = "initiator.username")
    @Mapping(target = "receiverUsername", source = "receiver.username")
    TradeSessionDto toDto(TradeSession tradeSession);
}