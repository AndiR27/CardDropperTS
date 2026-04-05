package ts.backend_carddropper.reroll.models;

import ts.backend_carddropper.models.CardDto;

public record RerollResponse(
        CardDto sacrificedCard,
        CardDto receivedCard
) {}
