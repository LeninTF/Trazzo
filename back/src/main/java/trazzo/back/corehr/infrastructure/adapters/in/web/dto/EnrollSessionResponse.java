package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record EnrollSessionResponse(
        @JsonProperty("enroll_token") String enrollToken,
        @JsonProperty("device_id") Long deviceId,
        @JsonProperty("tenant_user_id") Long tenantUserId,
        @JsonProperty("finger_index") Integer fingerIndex,
        @JsonProperty("device_code") String deviceCode,
        @JsonProperty("expires_at") LocalDateTime expiresAt
) {
    public static EnrollSessionResponse from(trazzo.back.corehr.infrastructure.adapters.out.enroll.EnrollSessionResponse result) {
        return new EnrollSessionResponse(result.enrollToken(), result.deviceId(),
                result.tenantUserId(), result.fingerIndex(), result.deviceCode(), result.expiresAt());
    }
}
