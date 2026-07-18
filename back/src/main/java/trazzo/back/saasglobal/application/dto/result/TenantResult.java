package trazzo.back.saasglobal.application.dto.result;

import java.time.LocalDateTime;

public record TenantResult(
        String id,
        String subDomain,
        Integer holdingId,
        String holdingName,
        Integer planId,
        String planName,
        String status,
        LocalDateTime activatedAt,
        LocalDateTime createdAt
) {}
