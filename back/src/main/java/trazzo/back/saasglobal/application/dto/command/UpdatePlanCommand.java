package trazzo.back.saasglobal.application.dto.command;

import java.math.BigDecimal;
import java.util.Map;

public record UpdatePlanCommand(
        Integer id,
        String name,
        BigDecimal price,
        BigDecimal priceAnnual,
        String currency,
        String billingPeriod,
        Map<String, Object> features
) {}
