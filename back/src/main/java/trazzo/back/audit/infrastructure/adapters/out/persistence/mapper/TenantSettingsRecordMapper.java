package trazzo.back.audit.infrastructure.adapters.out.persistence.mapper;

import trazzo.back.audit.domain.model.master.TenantSettingsRecord;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.TenantSettingsRecordEntity;

public final class TenantSettingsRecordMapper {

    private TenantSettingsRecordMapper() {
    }

    public static TenantSettingsRecordEntity toEntity(TenantSettingsRecord domain) {
        var entity = new TenantSettingsRecordEntity();
        entity.setId(domain.getId());
        entity.setTenantSettingId(domain.getTenantSettingId());
        entity.setDbName(domain.getDbName());
        entity.setDbHost(domain.getDbHost());
        entity.setDbUser(domain.getDbUser());
        entity.setDbPassword(domain.getDbPassword());
        entity.setUserId(domain.getUserId());
        entity.setChangeReason(domain.getChangeReason());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    public static TenantSettingsRecord toDomain(TenantSettingsRecordEntity entity) {
        return new TenantSettingsRecord(
                entity.getId(),
                entity.getTenantSettingId(),
                entity.getDbName(),
                entity.getDbHost(),
                entity.getDbUser(),
                entity.getDbPassword(),
                entity.getUserId(),
                entity.getChangeReason(),
                entity.getCreatedAt()
        );
    }
}
