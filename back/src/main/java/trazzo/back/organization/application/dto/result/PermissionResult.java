package trazzo.back.organization.application.dto.result;

import java.time.LocalDateTime;

public record PermissionResult(
        String id,
        String name,
        String description,
        String masterFeaturesCode,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
