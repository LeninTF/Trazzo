package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.NotNull;

public record AssignRoleRequest(
    @NotNull String roleId
) {}
