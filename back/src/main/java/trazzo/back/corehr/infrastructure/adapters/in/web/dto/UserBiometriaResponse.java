package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.corehr.application.dto.result.UserBiometriaResult;

import java.time.LocalDateTime;

public record UserBiometriaResponse(
        Long id,
        @JsonProperty("tenant_user_id") Long tenantUserId,
        @JsonProperty("device_id") Long deviceId,
        @JsonProperty("device_code") String deviceCode,
        @JsonProperty("finger_index") Integer fingerIndex,
        boolean activo,
        @JsonProperty("capturado_en") LocalDateTime capturadoEn,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt
) {
    public static UserBiometriaResponse from(UserBiometriaResult result) {
        return new UserBiometriaResponse(result.id(), result.tenantUserId(), result.deviceId(),
                result.deviceCode(), result.fingerIndex(), result.activo(), result.capturadoEn(),
                result.createdAt(), result.updatedAt());
    }
}
