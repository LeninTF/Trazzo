package trazzo.back.saasglobal.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.Map;

public record CreatePlanRequest(
        @NotBlank String name,
        @NotNull @PositiveOrZero BigDecimal price,
        @PositiveOrZero BigDecimal priceAnnual,
        @NotBlank String currency,
        String billingPeriod,
        Map<String, Object> features
) {}
