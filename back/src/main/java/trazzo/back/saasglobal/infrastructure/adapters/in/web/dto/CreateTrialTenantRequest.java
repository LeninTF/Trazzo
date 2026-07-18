package trazzo.back.saasglobal.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTrialTenantRequest(
        @NotBlank String subDomain,
        @NotNull Integer planId,
        @NotNull Integer holdingId,
        String logoUrl,
        String slogan,
        String primaryColor,
        String secondaryColor
) {}
