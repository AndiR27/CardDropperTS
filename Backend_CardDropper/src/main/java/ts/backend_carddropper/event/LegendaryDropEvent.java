package ts.backend_carddropper.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class LegendaryDropEvent extends ApplicationEvent {

    private final String actorUsername;

    public LegendaryDropEvent(Object source, String actorUsername) {
        super(source);
        this.actorUsername = actorUsername;
    }
}
