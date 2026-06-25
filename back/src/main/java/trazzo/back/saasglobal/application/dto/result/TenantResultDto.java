package trazzo.back.saasglobal.application.dto.result;

import java.time.LocalDateTime;

public record TenantResultDto(
        String id,
        String subDomain,
        Integer planId,
        boolean activated,
        LocalDateTime activatedAt,
        LocalDateTime createdAt
) {
}
