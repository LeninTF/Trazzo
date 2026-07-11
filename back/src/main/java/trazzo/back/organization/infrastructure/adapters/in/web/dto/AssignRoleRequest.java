package trazzo.back.organization.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignRoleRequest(
        @NotBlank String roleId,
        Long departmentId
) {}
