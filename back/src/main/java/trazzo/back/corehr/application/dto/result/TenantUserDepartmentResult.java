package trazzo.back.corehr.application.dto.result;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TenantUserDepartmentResult(
        Long id,
        Long tenantUserId,
        Long departmentId,
        String departmentName,
        boolean isPrimary,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
