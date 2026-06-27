package trazzo.back.corehr.domain.model.attendance;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.corehr.domain.exception.CoreHrValidationException;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBiometria {

    private Long id;
    private Long tenantUserId;
    private Long deviceId;
    private Integer fingerIndex;
    private LocalDateTime capturadoEn;
    private boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    transient Clock clock = Clock.systemDefaultZone();

    private UserBiometria(
            Long id,
            Long tenantUserId,
            Long deviceId,
            Integer fingerIndex,
            LocalDateTime capturadoEn,
            boolean activo,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.tenantUserId = requireTenantUserId(tenantUserId);
        this.deviceId = deviceId;
        this.fingerIndex = fingerIndex;
        this.capturadoEn = capturadoEn;
        this.activo = activo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserBiometria create(Long tenantUserId, Long deviceId, Integer fingerIndex, LocalDateTime capturadoEn) {
        LocalDateTime now = LocalDateTime.now();
        return new UserBiometria(null, tenantUserId, deviceId, fingerIndex, capturadoEn, true, now, now);
    }

    public static UserBiometria restore(
            Long id,
            Long tenantUserId,
            Long deviceId,
            Integer fingerIndex,
            LocalDateTime capturadoEn,
            boolean activo,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new UserBiometria(id, tenantUserId, deviceId, fingerIndex, capturadoEn, activo, createdAt, updatedAt);
    }

    public void activate() {
        this.activo = true;
        touch();
    }

    public void deactivate() {
        this.activo = false;
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now(clock);
    }

    private static Long requireTenantUserId(Long tenantUserId) {
        if (tenantUserId == null) {
            throw new CoreHrValidationException("tenantUserId is required");
        }
        return tenantUserId;
    }
}
