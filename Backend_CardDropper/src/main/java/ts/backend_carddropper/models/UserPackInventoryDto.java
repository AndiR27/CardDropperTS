package ts.backend_carddropper.models;

public record UserPackInventoryDto(
        Long templateId,
        String templateName,
        int quantity
) {}
