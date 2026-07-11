package trazzo.back.organization.application.dto.result;

import java.time.LocalDateTime;

public record DepartmentResult(
        Long id,
        Long areaId,
        String name,
        String description,
        boolean state,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt
) {}
