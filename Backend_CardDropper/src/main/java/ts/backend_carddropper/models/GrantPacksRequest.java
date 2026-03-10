package ts.backend_carddropper.models;

import java.util.List;

public record GrantPacksRequest(
        List<Long> userIds,
        Long templateId,
        int quantity
) {}
