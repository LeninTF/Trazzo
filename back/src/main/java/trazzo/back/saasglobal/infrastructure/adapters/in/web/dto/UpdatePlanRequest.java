package trazzo.back.saasglobal.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record UpdatePlanRequest(
        @NotBlank String name,
        @NotNull @PositiveOrZero BigDecimal price,
        @NotBlank String currency,
        String billingPeriod
) {}
