package trazzo.back.organization.application.dto.result;

import java.time.LocalDateTime;

public record RoleResult(
        String id,
        String name,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
