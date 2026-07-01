package trazzo.back.saasglobal.application.dto.command;

import java.math.BigDecimal;

public record CreatePlanCommand(
        String name,
        BigDecimal price,
        String currency,
        String billingPeriod
) {}
