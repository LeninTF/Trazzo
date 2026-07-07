package trazzo.back.organization.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignPermissionRequest(@NotBlank String permissionId) {}
