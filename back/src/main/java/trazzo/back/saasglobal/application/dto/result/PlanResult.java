package trazzo.back.saasglobal.application.dto.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PlanResult(
        Integer id,
        String name,
        BigDecimal price,
        String currency,
        String billingPeriod,
        boolean active,
        LocalDateTime createdAt
) {}
