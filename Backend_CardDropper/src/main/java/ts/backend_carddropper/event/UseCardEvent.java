package ts.backend_carddropper.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class UseCardEvent extends ApplicationEvent {

    private final String actorUsername;
    private final String cardName;
    private final String cardRarity;
    private final String targetUsername;

    public UseCardEvent(Object source, String actorUsername, String cardName,
                        String cardRarity, String targetUsername) {
        super(source);
        this.actorUsername = actorUsername;
        this.cardName = cardName;
        this.cardRarity = cardRarity;
        this.targetUsername = targetUsername;
    }
}
