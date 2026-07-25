package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.corehr.infrastructure.adapters.out.enroll.EnrollSession;

import java.time.LocalDateTime;

public record PendingEnrollSessionResponse(
        @JsonProperty("enroll_token") String enrollToken,
        @JsonProperty("device_id") Long deviceId,
        @JsonProperty("device_code") String deviceCode,
        @JsonProperty("tenant_user_id") Long tenantUserId,
        @JsonProperty("finger_index") Integer fingerIndex,
        @JsonProperty("expires_at") LocalDateTime expiresAt
) {
        public static PendingEnrollSessionResponse from(EnrollSession session) {
                return new PendingEnrollSessionResponse(session.enrollToken(), session.deviceId(), session.deviceCode(), session.tenantUserId(), session.fingerIndex(), session.expiresAt());
        }
}