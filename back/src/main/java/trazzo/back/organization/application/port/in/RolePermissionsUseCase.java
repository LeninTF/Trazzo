package trazzo.back.organization.application.port.in;

import trazzo.back.organization.application.dto.command.AssignPermissionToRoleCommand;
import trazzo.back.organization.application.dto.result.RolePermissionResult;

import java.util.List;

public interface RolePermissionsUseCase {
    RolePermissionResult assign(String roleId, AssignPermissionToRoleCommand command);
    List<RolePermissionResult> findByRoleId(String roleId);
    void remove(String roleId, String permissionId);
}
