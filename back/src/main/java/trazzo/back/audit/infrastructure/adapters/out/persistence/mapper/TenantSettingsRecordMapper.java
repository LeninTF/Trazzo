package trazzo.back.audit.infrastructure.adapters.out.persistence.mapper;

import trazzo.back.audit.domain.model.master.TenantSettingsRecord;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.TenantSettingsRecordEntity;

import java.util.UUID;

public final class TenantSettingsRecordMapper {

    private TenantSettingsRecordMapper() {
    }

    private static UUID toUUID(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static TenantSettingsRecordEntity toEntity(TenantSettingsRecord domain) {
        var entity = new TenantSettingsRecordEntity();
        entity.setId(domain.getId());
        entity.setTenantSettingId(toUUID(domain.getTenantSettingId()));
        entity.setDbName(domain.getDbName());
        entity.setDbHost(domain.getDbHost());
        entity.setDbUser(domain.getDbUser());
        entity.setDbPassword(domain.getDbPassword());
        entity.setUserId(toUUID(domain.getUserId()));
        entity.setChangeReason(domain.getChangeReason());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    public static TenantSettingsRecord toDomain(TenantSettingsRecordEntity entity) {
        return new TenantSettingsRecord(
                entity.getId(),
                entity.getTenantSettingId() != null ? entity.getTenantSettingId().toString() : null,
                entity.getDbName(),
                entity.getDbHost(),
                entity.getDbUser(),
                entity.getDbPassword(),
                entity.getUserId() != null ? entity.getUserId().toString() : null,
                entity.getChangeReason(),
                entity.getCreatedAt()
        );
    }
}
