package trazzo.back.organization.application.port.out;

import trazzo.back.organization.domain.model.roles.RolePermissions;

import java.util.List;

public interface RolePermissionsRepositoryPort {
    RolePermissions save(RolePermissions rolePermissions);
    List<RolePermissions> findByRoleId(String roleId);
    boolean existsByRoleIdAndPermissionId(String roleId, String permissionId);
    void deleteByRoleIdAndPermissionId(String roleId, String permissionId);
}
