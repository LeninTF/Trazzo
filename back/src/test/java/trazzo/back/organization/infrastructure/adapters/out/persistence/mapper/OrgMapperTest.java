package trazzo.back.organization.infrastructure.adapters.out.persistence.mapper;

import org.junit.jupiter.api.Test;
import trazzo.back.organization.domain.model.business.Area;
import trazzo.back.organization.domain.model.business.Branch;
import trazzo.back.organization.domain.model.business.Department;
import trazzo.back.organization.domain.model.roles.Permissions;
import trazzo.back.organization.domain.model.roles.Role;
import trazzo.back.organization.domain.model.roles.RolePermissions;
import trazzo.back.organization.domain.model.roles.TenantUserRole;
import trazzo.back.organization.infrastructure.adapters.out.persistence.entity.*;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrgMapperTest {

    private final LocalDateTime now = LocalDateTime.of(2024, 1, 15, 10, 0);

    private static final String ROLE_UUID = "00000000-0000-0000-0000-000000000001";
    private static final String PERM_UUID = "00000000-0000-0000-0000-000000000002";
    private static final UUID ROLE_UUID_OBJ = UUID.fromString(ROLE_UUID);
    private static final UUID PERM_UUID_OBJ = UUID.fromString(PERM_UUID);

    // ── Branch ──────────────────────────────────────────────────────────────

    @Test
    void toEntity_branch_mapsAllFields() {
        var domain = Branch.restore(1L, "HQ", "Headquarters", true, now, now, null);
        var entity = OrgMapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getName()).isEqualTo("HQ");
        assertThat(entity.getDescription()).isEqualTo("Headquarters");
        assertThat(entity.getState()).isTrue();
        assertThat(entity.getCreatedAt()).isEqualTo(now);
        assertThat(entity.getUpdatedAt()).isEqualTo(now);
        assertThat(entity.getDeletedAt()).isNull();
    }

    @Test
    void toDomain_branch_mapsAllFields() {
        var entity = new BranchEntity();
        entity.setId(2L);
        entity.setName("Branch2");
        entity.setDescription("desc");
        entity.setState(false);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setDeletedAt(now);

        var domain = OrgMapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo(2L);
        assertThat(domain.getName()).isEqualTo("Branch2");
        assertThat(domain.getDescription()).isEqualTo("desc");
        assertThat(domain.isState()).isFalse();
        assertThat(domain.getDeletedAt()).isEqualTo(now);
    }

    @Test
    void toDomain_branch_nullState_mapsFalse() {
        var entity = new BranchEntity();
        entity.setId(3L);
        entity.setName("X");
        entity.setState(null);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        var domain = OrgMapper.toDomain(entity);
        assertThat(domain.isState()).isFalse();
    }

    // ── Area ────────────────────────────────────────────────────────────────

    @Test
    void toEntity_area_mapsAllFields() {
        var domain = Area.restore(1L, 2L, "Sales", "desc", true, now, now, null);
        var entity = OrgMapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getBranchId()).isEqualTo(2L);
        assertThat(entity.getName()).isEqualTo("Sales");
        assertThat(entity.getDescription()).isEqualTo("desc");
        assertThat(entity.getState()).isTrue();
        assertThat(entity.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void toDomain_area_mapsAllFields() {
        var entity = new AreaEntity();
        entity.setId(1L);
        entity.setBranchId(2L);
        entity.setName("Finance");
        entity.setDescription("fin dept");
        entity.setState(true);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setDeletedAt(null);

        var domain = OrgMapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo(1L);
        assertThat(domain.getBranchId()).isEqualTo(2L);
        assertThat(domain.getName()).isEqualTo("Finance");
        assertThat(domain.isState()).isTrue();
    }

    // ── Department ──────────────────────────────────────────────────────────

    @Test
    void toEntity_department_mapsAllFields() {
        var domain = Department.restore(10L, 5L, "HR", "desc", true, now, now, null);
        var entity = OrgMapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo(10L);
        assertThat(entity.getAreaId()).isEqualTo(5L);
        assertThat(entity.getName()).isEqualTo("HR");
        assertThat(entity.getState()).isTrue();
        assertThat(entity.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void toDomain_department_mapsAllFields() {
        var entity = new DepartmentEntity();
        entity.setId(3L);
        entity.setAreaId(4L);
        entity.setName("Payroll");
        entity.setDescription("payroll dept");
        entity.setState(false);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setDeletedAt(now);

        var domain = OrgMapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo(3L);
        assertThat(domain.getAreaId()).isEqualTo(4L);
        assertThat(domain.getName()).isEqualTo("Payroll");
        assertThat(domain.isState()).isFalse();
    }

    // ── Role ────────────────────────────────────────────────────────────────

    @Test
    void toEntity_role_mapsAllFields() {
        var domain = Role.restore(ROLE_UUID, "Admin", "admin role", now, now);
        var entity = OrgMapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo(ROLE_UUID_OBJ);
        assertThat(entity.getName()).isEqualTo("Admin");
        assertThat(entity.getDescription()).isEqualTo("admin role");
        assertThat(entity.getCreatedAt()).isEqualTo(now);
        assertThat(entity.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    void toDomain_role_mapsAllFields() {
        var entity = new RoleEntity();
        entity.setId(ROLE_UUID_OBJ);
        entity.setName("Viewer");
        entity.setDescription("read only");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        var domain = OrgMapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo(ROLE_UUID);
        assertThat(domain.getName()).isEqualTo("Viewer");
        assertThat(domain.getDescription()).isEqualTo("read only");
    }

    // ── Permissions ─────────────────────────────────────────────────────────

    @Test
    void toEntity_permissions_mapsAllFields() {
        var domain = Permissions.restore(PERM_UUID, "READ", "read perm", "CODE_READ", now, now);
        var entity = OrgMapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo(PERM_UUID_OBJ);
        assertThat(entity.getName()).isEqualTo("READ");
        assertThat(entity.getDescription()).isEqualTo("read perm");
        assertThat(entity.getMasterFeaturesCode()).isEqualTo("CODE_READ");
        assertThat(entity.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void toDomain_permissions_mapsAllFields() {
        var entity = new PermissionEntity();
        entity.setId(PERM_UUID_OBJ);
        entity.setName("WRITE");
        entity.setDescription("write access");
        entity.setMasterFeaturesCode("CODE_WRITE");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        var domain = OrgMapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo(PERM_UUID);
        assertThat(domain.getName()).isEqualTo("WRITE");
        assertThat(domain.getMasterFeaturesCode()).isEqualTo("CODE_WRITE");
    }

    // ── RolePermissions ─────────────────────────────────────────────────────

    @Test
    void toEntity_rolePermissions_mapsCompositeKey() {
        var domain = RolePermissions.restore(ROLE_UUID, PERM_UUID, now);
        var entity = OrgMapper.toEntity(domain);

        assertThat(entity.getId().getRoleId()).isEqualTo(ROLE_UUID_OBJ);
        assertThat(entity.getId().getPermissionId()).isEqualTo(PERM_UUID_OBJ);
        assertThat(entity.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void toDomain_rolePermissions_mapsCompositeKey() {
        var id = new RolePermissionsId(ROLE_UUID_OBJ, PERM_UUID_OBJ);
        var entity = new RolePermissionsEntity(id, now);

        var domain = OrgMapper.toDomain(entity);

        assertThat(domain.getRoleId()).isEqualTo(ROLE_UUID);
        assertThat(domain.getPermissionId()).isEqualTo(PERM_UUID);
        assertThat(domain.getCreatedAt()).isEqualTo(now);
    }

    // ── TenantUserRole ──────────────────────────────────────────────────────

    @Test
    void toEntity_tenantUserRole_mapsAllFields() {
        var domain = TenantUserRole.restore(10L, 20L, ROLE_UUID, 30L, now);
        var entity = OrgMapper.toEntity(domain);

        assertThat(entity.getId()).isEqualTo(10L);
        assertThat(entity.getTenantUserId()).isEqualTo(20L);
        assertThat(entity.getRoleId()).isEqualTo(ROLE_UUID_OBJ);
        assertThat(entity.getDepartmentId()).isEqualTo(30L);
        assertThat(entity.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void toEntity_tenantUserRole_nullDepartmentId_mapsNull() {
        var domain = TenantUserRole.restore(11L, 21L, ROLE_UUID, null, now);
        var entity = OrgMapper.toEntity(domain);

        assertThat(entity.getDepartmentId()).isNull();
    }

    @Test
    void toDomain_tenantUserRole_mapsAllFields() {
        var entity = new TenantUserRoleEntity();
        entity.setId(12L);
        entity.setTenantUserId(22L);
        entity.setRoleId(ROLE_UUID_OBJ);
        entity.setDepartmentId(32L);
        entity.setCreatedAt(now);

        var domain = OrgMapper.toDomain(entity);

        assertThat(domain.getId()).isEqualTo(12L);
        assertThat(domain.getTenantUserId()).isEqualTo(22L);
        assertThat(domain.getRoleId()).isEqualTo(ROLE_UUID);
        assertThat(domain.getDepartmentId()).isEqualTo(32L);
    }
}
