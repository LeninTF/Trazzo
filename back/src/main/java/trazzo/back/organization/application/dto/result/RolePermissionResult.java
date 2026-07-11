package trazzo.back.organization.application.dto.result;

import java.time.LocalDateTime;

public record RolePermissionResult(
        String roleId,
        String permissionId,
        LocalDateTime createdAt
) {}
