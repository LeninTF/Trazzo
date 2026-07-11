package trazzo.back.organization.application.dto.result;

import java.time.LocalDateTime;

public record UserRoleAssignmentResult(
        Long id,
        Long tenantUserId,
        String roleId,
        Long departmentId,
        LocalDateTime createdAt
) {}
