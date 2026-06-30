package trazzo.back.audit.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.audit.application.dto.result.SessionResult;
import trazzo.back.audit.domain.model.tenant.SessionState;
import java.time.LocalDateTime;

public record SessionResponse(
    Long id,
    @JsonProperty("tenant_user_id") String tenantUserId,
    @JsonProperty("refresh_token_hash") String refreshTokenHash,
    @JsonProperty("ip_address") String ipAddress,
    @JsonProperty("user_agent") String userAgent,
    @JsonProperty("device_fingerprint") String deviceFingerprint,
    @JsonProperty("login_at") LocalDateTime loginAt,
    @JsonProperty("last_activity_at") LocalDateTime lastActivityAt,
    @JsonProperty("logout_at") LocalDateTime logoutAt,
    @JsonProperty("expires_at") LocalDateTime expiresAt,
    SessionState state,
    @JsonProperty("created_at") LocalDateTime createdAt,
    @JsonProperty("updated_at") LocalDateTime updatedAt
) {
    public static SessionResponse from(SessionResult result) {
        return new SessionResponse(
            result.id(), result.tenantUserId(), result.refreshTokenHash(),
            result.ipAddress(), result.userAgent(), result.deviceFingerprint(),
            result.loginAt(), result.lasActivityAt(), result.logoutAt(),
            result.expiresAt(), result.state(),
            result.createdAt(), result.updatedAt()
        );
    }
}
