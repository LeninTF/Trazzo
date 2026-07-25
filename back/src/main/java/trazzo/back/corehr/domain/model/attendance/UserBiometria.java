package trazzo.back.corehr.domain.model.attendance;

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
    private String deviceCode;
    private Integer fingerIndex;
    private String encryptedTemplateBase64;
    private String encryptedAesKeyBase64;
    private String ivBase64;
    private String tagBase64;
    private LocalDateTime capturadoEn;
    private boolean activo;

    private UserBiometria(
            Long id,
            Long tenantUserId,
            Long deviceId,
            String deviceCode,
            Integer fingerIndex,
            String encryptedTemplateBase64,
            String encryptedAesKeyBase64,
            String ivBase64,
            String tagBase64,
            LocalDateTime capturadoEn,
            boolean activo,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        super(id, createdAt, updatedAt);
        this.tenantUserId = DomainModelValidator.requireTenantUserId(tenantUserId);
        this.deviceId = deviceId;
        this.deviceCode = deviceCode;
        this.fingerIndex = fingerIndex;
        this.encryptedTemplateBase64 = DomainModelValidator.requireText(encryptedTemplateBase64, "encryptedTemplateBase64");
        this.encryptedAesKeyBase64 = DomainModelValidator.requireText(encryptedAesKeyBase64, "encryptedAesKeyBase64");
        this.ivBase64 = ivBase64;
        this.tagBase64 = tagBase64;
        this.capturadoEn = capturadoEn;
        this.activo = activo;
    }

    public static UserBiometria create(
            Long tenantUserId,
            Long deviceId,
            String deviceCode,
            Integer fingerIndex,
            String encryptedTemplateBase64,
            String encryptedAesKeyBase64,
            String ivBase64,
            String tagBase64,
            LocalDateTime capturadoEn
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new UserBiometria(null, tenantUserId, deviceId, deviceCode, fingerIndex,
                encryptedTemplateBase64, encryptedAesKeyBase64, ivBase64, tagBase64,
                capturadoEn, true, now, now);
    }

    public static UserBiometria restore(
            Long id,
            Long tenantUserId,
            Long deviceId,
            String deviceCode,
            Integer fingerIndex,
            String encryptedTemplateBase64,
            String encryptedAesKeyBase64,
            String ivBase64,
            String tagBase64,
            LocalDateTime capturadoEn,
            boolean activo,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new UserBiometria(id, tenantUserId, deviceId, deviceCode, fingerIndex,
                encryptedTemplateBase64, encryptedAesKeyBase64, ivBase64, tagBase64,
                capturadoEn, activo, createdAt, updatedAt);
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
