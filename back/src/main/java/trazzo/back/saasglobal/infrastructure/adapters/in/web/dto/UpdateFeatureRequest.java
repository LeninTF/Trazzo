package trazzo.back.saasglobal.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateFeatureRequest(
        @NotBlank String name,
        String description
) {}
