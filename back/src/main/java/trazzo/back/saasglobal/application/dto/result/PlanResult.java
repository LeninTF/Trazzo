package trazzo.back.saasglobal.application.dto.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record PlanResult(
        Integer id,
        String name,
        BigDecimal price,
        BigDecimal priceAnnual,
        String currency,
        String billingPeriod,
        boolean active,
        LocalDateTime createdAt,
        Map<String, Object> features
) {}
