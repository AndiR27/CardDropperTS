package ts.backend_carddropper.models;

import java.util.List;

public record PackTemplateDto(
        Long id,
        String name,
        List<PackTemplateSlotDto> slots
) {
}
