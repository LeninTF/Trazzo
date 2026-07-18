package trazzo.back.saasglobal.application.dto.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InvoiceResult(
        String id,
        String tenantId,
        String invoiceSeries,
        String consecutiveNumber,
        String voucherType,
        String clientTaxId,
        String clientName,
        BigDecimal subTotal,
        BigDecimal taxAmount,
        BigDecimal total,
        String paymentStatus,
        LocalDate expirationDate,
        LocalDateTime createdAt
) {}
