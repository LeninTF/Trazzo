package trazzo.back.corehr.domain.model.attendance;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.corehr.domain.model.BaseDomainModel;
import trazzo.back.corehr.domain.model.DomainModelValidator;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBiometria extends BaseDomainModel {

    private Long tenantUserId;
    private Long deviceId;
    private Integer fingerIndex;
    private String templateCifrado;
    private String llaveCifrado;
    private LocalDateTime capturadoEn;
    private boolean activo;

    private UserBiometria(
            Long id,
            Long tenantUserId,
            Long deviceId,
            Integer fingerIndex,
            String templateCifrado,
            String llaveCifrado,
            LocalDateTime capturadoEn,
            boolean activo,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        super(id, createdAt, updatedAt);
        this.tenantUserId = DomainModelValidator.requireTenantUserId(tenantUserId);
        this.deviceId = deviceId;
        this.fingerIndex = fingerIndex;
        this.templateCifrado = DomainModelValidator.requireText(templateCifrado, "templateCifrado");
        this.llaveCifrado = DomainModelValidator.requireText(llaveCifrado, "llaveCifrado");
        this.capturadoEn = capturadoEn;
        this.activo = activo;
    }

    public static UserBiometria create(Long tenantUserId, Long deviceId, Integer fingerIndex,
                                       String templateCifrado, String llaveCifrado, LocalDateTime capturadoEn) {
        LocalDateTime now = LocalDateTime.now();
        return new UserBiometria(null, tenantUserId, deviceId, fingerIndex, templateCifrado,
                llaveCifrado, capturadoEn, true, now, now);
    }

    public static UserBiometria restore(
            Long id,
            Long tenantUserId,
            Long deviceId,
            Integer fingerIndex,
            String templateCifrado,
            String llaveCifrado,
            LocalDateTime capturadoEn,
            boolean activo,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new UserBiometria(id, tenantUserId, deviceId, fingerIndex, templateCifrado,
                llaveCifrado, capturadoEn, activo, createdAt, updatedAt);
    }

    public void activate() {
        this.activo = true;
        touch();
    }

    public void deactivate() {
        this.activo = false;
        touch();
    }
}
