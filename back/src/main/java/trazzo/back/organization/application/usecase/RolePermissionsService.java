package trazzo.back.organization.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.organization.application.dto.command.AssignPermissionToRoleCommand;
import trazzo.back.organization.application.dto.result.RolePermissionResult;
import trazzo.back.organization.application.port.in.RolePermissionsUseCase;
import trazzo.back.organization.application.port.out.PermissionRepositoryPort;
import trazzo.back.organization.application.port.out.RolePermissionsRepositoryPort;
import trazzo.back.organization.application.port.out.RoleRepositoryPort;
import trazzo.back.organization.domain.exception.DuplicateOrgNameException;
import trazzo.back.organization.domain.exception.OrgNotFoundException;
import trazzo.back.organization.domain.model.roles.RolePermissions;

import java.util.List;

@RequiredArgsConstructor
@Transactional
public class RolePermissionsService implements RolePermissionsUseCase {

    private final RolePermissionsRepositoryPort rolePermissionsRepository;
    private final RoleRepositoryPort roleRepository;
    private final PermissionRepositoryPort permissionRepository;

    @Override
    public RolePermissionResult assign(String roleId, AssignPermissionToRoleCommand command) {
        roleRepository.findById(roleId)
                .orElseThrow(() -> new OrgNotFoundException("Role not found: " + roleId));
        permissionRepository.findById(command.permissionId())
                .orElseThrow(() -> new OrgNotFoundException("Permission not found: " + command.permissionId()));
        if (rolePermissionsRepository.existsByRoleIdAndPermissionId(roleId, command.permissionId())) {
            throw new DuplicateOrgNameException("Permission already assigned to this role");
        }
        var rp = RolePermissions.create(roleId, command.permissionId());
        return toResult(rolePermissionsRepository.save(rp));
    }

    @Override
    public List<RolePermissionResult> findByRoleId(String roleId) {
        roleRepository.findById(roleId)
                .orElseThrow(() -> new OrgNotFoundException("Role not found: " + roleId));
        return rolePermissionsRepository.findByRoleId(roleId).stream().map(this::toResult).toList();
    }

    @Override
    public void remove(String roleId, String permissionId) {
        if (!rolePermissionsRepository.existsByRoleIdAndPermissionId(roleId, permissionId)) {
            throw new OrgNotFoundException("Permission not assigned to this role");
        }
        rolePermissionsRepository.deleteByRoleIdAndPermissionId(roleId, permissionId);
    }

    private RolePermissionResult toResult(RolePermissions rp) {
        return new RolePermissionResult(rp.getRoleId(), rp.getPermissionId(), rp.getCreatedAt());
    }
}
