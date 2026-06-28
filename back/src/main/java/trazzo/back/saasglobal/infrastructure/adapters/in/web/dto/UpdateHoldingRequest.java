package trazzo.back.saasglobal.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateHoldingRequest(
        @NotBlank String legalName,
        @NotBlank String type
) {}
