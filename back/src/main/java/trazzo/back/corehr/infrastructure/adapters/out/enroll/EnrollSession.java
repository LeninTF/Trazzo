package trazzo.back.corehr.infrastructure.adapters.out.enroll;

import java.time.LocalDateTime;

public record EnrollSession(
        String enrollToken,
        Long tenantUserId,
        Long deviceId,
        Integer fingerIndex,
        String deviceCode,
        LocalDateTime expiresAt
) {}
