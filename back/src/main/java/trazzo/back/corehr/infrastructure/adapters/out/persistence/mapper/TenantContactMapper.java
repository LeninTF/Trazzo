package trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper;

import trazzo.back.corehr.domain.model.employee.TenantContact;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.TenantContactEntity;

public final class TenantContactMapper {

    private TenantContactMapper() {
    }

    public static TenantContactEntity toEntity(TenantContact domain) {
        var entity = new TenantContactEntity();
        entity.setId(domain.getId());
        entity.setTenantUserId(domain.getTenantUserId());
        entity.setType(domain.getType());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setDeletedAt(domain.getDeletedAt());
        return entity;
    }

    public static TenantContact toDomain(TenantContactEntity entity) {
        return TenantContact.restore(
                entity.getId(),
                entity.getTenantUserId(),
                entity.getType(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt()
        );
    }
}
