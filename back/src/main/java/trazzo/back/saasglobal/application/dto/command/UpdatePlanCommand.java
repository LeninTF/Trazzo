package trazzo.back.saasglobal.application.dto.command;

import java.math.BigDecimal;

public record UpdatePlanCommand(
        Integer id,
        String name,
        BigDecimal price,
        String currency,
        String billingPeriod
) {}
