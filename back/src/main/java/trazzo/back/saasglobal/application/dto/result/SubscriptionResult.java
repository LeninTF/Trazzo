package trazzo.back.saasglobal.application.dto.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SubscriptionResult(
        String id,
        String tenantId,
        String tenantName,
        Integer planId,
        String planName,
        LocalDate dateStart,
        LocalDate dateEnd,
        String status,
        BigDecimal purchasePrice,
        LocalDateTime createdAt
) {}
