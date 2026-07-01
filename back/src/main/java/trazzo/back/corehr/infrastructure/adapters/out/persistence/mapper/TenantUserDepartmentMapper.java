package trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper;

import trazzo.back.corehr.domain.model.employee.TenantUserDepartment;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.TenantUserDepartmentEntity;

public final class TenantUserDepartmentMapper {

    private TenantUserDepartmentMapper() {
    }

    public static TenantUserDepartmentEntity toEntity(TenantUserDepartment domain) {
        var entity = new TenantUserDepartmentEntity();
        entity.setId(domain.getId());
        entity.setTenantUserId(domain.getTenantUserId());
        entity.setDepartmentId(domain.getDepartmentId());
        entity.setPrimary(domain.isPrimary());
        entity.setStartDate(domain.getStartDate());
        entity.setEndDate(domain.getEndDate());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public static TenantUserDepartment toDomain(TenantUserDepartmentEntity entity) {
        return TenantUserDepartment.restore(
                entity.getId(),
                entity.getTenantUserId(),
                entity.getDepartmentId(),
                entity.isPrimary(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
