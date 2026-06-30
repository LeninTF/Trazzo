package trazzo.back.organization.application.dto.result;

import java.time.LocalDateTime;

public record AreaResult(
        Long id,
        Long branchId,
        String name,
        String description,
        boolean state,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {}
