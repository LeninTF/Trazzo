package trazzo.back.saasglobal.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateHoldingRequest(
        @NotBlank String taxId,
        @NotBlank String legalName,
        @NotBlank String type
) {}
