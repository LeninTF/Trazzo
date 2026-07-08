package trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper;

import trazzo.back.corehr.domain.model.attendance.UserBiometria;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.UserBiometriaEntity;

public final class UserBiometriaMapper {

    private UserBiometriaMapper() {
    }

    public static UserBiometriaEntity toEntity(UserBiometria domain) {
        var entity = new UserBiometriaEntity();
        entity.setId(domain.getId());
        entity.setTenantUserId(domain.getTenantUserId());
        entity.setDeviceId(domain.getDeviceId());
        entity.setDeviceCode(domain.getDeviceCode());
        entity.setFingerIndex(domain.getFingerIndex());
        entity.setEncryptedTemplateBase64(domain.getEncryptedTemplateBase64());
        entity.setEncryptedAesKeyBase64(domain.getEncryptedAesKeyBase64());
        entity.setIvBase64(domain.getIvBase64());
        entity.setTagBase64(domain.getTagBase64());
        entity.setCapturadoEn(domain.getCapturadoEn());
        entity.setActivo(domain.isActivo());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public static UserBiometria toDomain(UserBiometriaEntity entity) {
        return UserBiometria.restore(
                entity.getId(),
                entity.getTenantUserId(),
                entity.getDeviceId(),
            entity.getDeviceCode(),
                entity.getFingerIndex(),
            entity.getEncryptedTemplateBase64(),
            entity.getEncryptedAesKeyBase64(),
            entity.getIvBase64(),
            entity.getTagBase64(),
                entity.getCapturadoEn(),
                entity.isActivo(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
