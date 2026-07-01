package trazzo.back.corehr.application.dto.result;

import java.time.LocalDateTime;

public record UserBiometriaResult(
        Long id,
        Long tenantUserId,
        Long deviceId,
        String deviceCode,
        Integer fingerIndex,
        boolean activo,
        LocalDateTime capturadoEn,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
