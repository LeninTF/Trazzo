package trazzo.back.audit.application.dto.result;

import trazzo.back.audit.domain.model.tenant.SessionState;
import java.time.LocalDateTime;

public record SessionResult(
    Long id,
    String tenantUserId,
    String refreshTokenHash,
    String ipAddress,
    String userAgent,
    String deviceFingerprint,
    LocalDateTime loginAt,
    LocalDateTime lastActivityAt,
    LocalDateTime logoutAt,
    LocalDateTime expiresAt,
    SessionState state,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
