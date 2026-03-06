package ts.backend_carddropper.models;

public record PackTemplateSlotDto(
        Long id,
        Long slotId,
        String slotName,
        int count
) {
}
