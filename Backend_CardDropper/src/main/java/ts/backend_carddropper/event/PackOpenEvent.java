package ts.backend_carddropper.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class PackOpenEvent extends ApplicationEvent {

    private final String actorUsername;
    private final String cardName;
    private final String cardRarity;
    private final String packName;

    public PackOpenEvent(Object source, String actorUsername, String cardName,
                         String cardRarity, String packName) {
        super(source);
        this.actorUsername = actorUsername;
        this.cardName = cardName;
        this.cardRarity = cardRarity;
        this.packName = packName;
    }
}
