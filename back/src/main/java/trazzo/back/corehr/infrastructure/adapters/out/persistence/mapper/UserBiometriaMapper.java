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
        entity.setFingerIndex(domain.getFingerIndex());
        entity.setTemplateCifrado(domain.getTemplateCifrado());
        entity.setLlaveCifrado(domain.getLlaveCifrado());
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
                entity.getFingerIndex(),
                entity.getTemplateCifrado(),
                entity.getLlaveCifrado(),
                entity.getCapturadoEn(),
                entity.isActivo(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
