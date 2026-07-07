package trazzo.back.organization.infrastructure.adapters.out.persistence.mapper;

import trazzo.back.organization.domain.model.business.Area;
import trazzo.back.organization.domain.model.business.Branch;
import trazzo.back.organization.domain.model.business.Department;
import trazzo.back.organization.domain.model.roles.Permissions;
import trazzo.back.organization.domain.model.roles.Role;
import trazzo.back.organization.domain.model.roles.RolePermissions;
import trazzo.back.organization.domain.model.roles.TenantUserRole;
import trazzo.back.organization.infrastructure.adapters.out.persistence.entity.*;

import java.util.UUID;

public final class OrgMapper {

    private OrgMapper() {}

    // ── Branch ──────────────────────────────────────────────────────────────

    public static BranchEntity toEntity(Branch domain) {
        var entity = new BranchEntity();
        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setState(domain.isState());
        entity.setDeletedAt(domain.getDeletedAt());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public static Branch toDomain(BranchEntity entity) {
        return Branch.restore(
                entity.getId(), entity.getName(), entity.getDescription(),
                Boolean.TRUE.equals(entity.getState()),
                entity.getCreatedAt(), entity.getUpdatedAt(), entity.getDeletedAt()
        );
    }

    // ── Area ────────────────────────────────────────────────────────────────

    public static AreaEntity toEntity(Area domain) {
        var entity = new AreaEntity();
        entity.setId(domain.getId());
        entity.setBranchId(domain.getBranchId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setState(domain.isState());
        entity.setDeletedAt(domain.getDeletedAt());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public static Area toDomain(AreaEntity entity) {
        return Area.restore(
                entity.getId(), entity.getBranchId(), entity.getName(), entity.getDescription(),
                Boolean.TRUE.equals(entity.getState()),
                entity.getCreatedAt(), entity.getUpdatedAt(), entity.getDeletedAt()
        );
    }

    // ── Department ──────────────────────────────────────────────────────────

    public static DepartmentEntity toEntity(Department domain) {
        var entity = new DepartmentEntity();
        entity.setId(domain.getId());
        entity.setAreaId(domain.getAreaId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setState(domain.isState());
        entity.setDeletedAt(domain.getDeletedAt());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public static Department toDomain(DepartmentEntity entity) {
        return Department.restore(
                entity.getId(), entity.getAreaId(), entity.getName(), entity.getDescription(),
                Boolean.TRUE.equals(entity.getState()),
                entity.getCreatedAt(), entity.getUpdatedAt(), entity.getDeletedAt()
        );
    }

    // ── Role ────────────────────────────────────────────────────────────────

    public static RoleEntity toEntity(Role domain) {
        var entity = new RoleEntity();
        entity.setId(domain.getId() != null ? UUID.fromString(domain.getId()) : null);
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public static Role toDomain(RoleEntity entity) {
        return Role.restore(
                entity.getId() != null ? entity.getId().toString() : null,
                entity.getName(), entity.getDescription(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    // ── Permission ──────────────────────────────────────────────────────────

    public static PermissionEntity toEntity(Permissions domain) {
        var entity = new PermissionEntity();
        entity.setId(domain.getId() != null ? UUID.fromString(domain.getId()) : null);
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setMasterFeaturesCode(domain.getMasterFeaturesCode());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public static Permissions toDomain(PermissionEntity entity) {
        return Permissions.restore(
                entity.getId() != null ? entity.getId().toString() : null,
                entity.getName(), entity.getDescription(),
                entity.getMasterFeaturesCode(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    // ── RolePermissions ─────────────────────────────────────────────────────

    public static RolePermissionsEntity toEntity(RolePermissions domain) {
        var id = new RolePermissionsId(
                UUID.fromString(domain.getRoleId()),
                UUID.fromString(domain.getPermissionId()));
        return new RolePermissionsEntity(id, domain.getCreatedAt());
    }

    public static RolePermissions toDomain(RolePermissionsEntity entity) {
        return RolePermissions.restore(
                entity.getId().getRoleId().toString(),
                entity.getId().getPermissionId().toString(),
                entity.getCreatedAt()
        );
    }

    // ── TenantUserRole ──────────────────────────────────────────────────────

    public static TenantUserRoleEntity toEntity(TenantUserRole domain) {
        var entity = new TenantUserRoleEntity();
        entity.setId(domain.getId());
        entity.setTenantUserId(domain.getTenantUserId());
        entity.setRoleId(UUID.fromString(domain.getRoleId()));
        entity.setDepartmentId(domain.getDepartmentId());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    public static TenantUserRole toDomain(TenantUserRoleEntity entity) {
        return TenantUserRole.restore(
                entity.getId(), entity.getTenantUserId(), entity.getRoleId().toString(),
                entity.getDepartmentId(), entity.getCreatedAt()
        );
    }
}
