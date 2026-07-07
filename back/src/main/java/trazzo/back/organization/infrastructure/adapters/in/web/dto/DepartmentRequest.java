package trazzo.back.organization.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
        @NotNull Long areaId,
        @NotBlank @Size(max = 255) String name,
        String description
) {}
