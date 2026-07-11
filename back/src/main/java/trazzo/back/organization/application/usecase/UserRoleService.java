package trazzo.back.organization.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.organization.application.dto.command.AssignRoleToUserCommand;
import trazzo.back.organization.application.dto.result.UserRoleAssignmentResult;
import trazzo.back.organization.application.port.in.UserRoleUseCase;
import trazzo.back.organization.application.port.out.RoleRepositoryPort;
import trazzo.back.organization.application.port.out.UserRoleRepositoryPort;
import trazzo.back.organization.domain.exception.DuplicateOrgNameException;
import trazzo.back.organization.domain.exception.OrgNotFoundException;
import trazzo.back.organization.domain.model.roles.TenantUserRole;

import java.util.List;

@RequiredArgsConstructor
public class UserRoleService implements UserRoleUseCase {

    private final UserRoleRepositoryPort userRoleRepository;
    private final RoleRepositoryPort roleRepository;

    @Override
    public UserRoleAssignmentResult assign(Long tenantUserId, AssignRoleToUserCommand command) {
        roleRepository.findById(command.roleId())
                .orElseThrow(() -> new OrgNotFoundException("Role not found: " + command.roleId()));
        if (userRoleRepository.existsByTenantUserIdAndRoleIdAndDepartmentId(
                tenantUserId, command.roleId(), command.departmentId())) {
            throw new DuplicateOrgNameException("Role already assigned to this user with the same department scope");
        }
        var assignment = TenantUserRole.create(tenantUserId, command.roleId(), command.departmentId());
        return toResult(userRoleRepository.save(assignment));
    }

    @Override
    public List<UserRoleAssignmentResult> findByTenantUserId(Long tenantUserId) {
        return userRoleRepository.findByTenantUserId(tenantUserId).stream().map(this::toResult).toList();
    }

    @Override
    public List<UserRoleAssignmentResult> findByRoleId(String roleId) {
        roleRepository.findById(roleId)
                .orElseThrow(() -> new OrgNotFoundException("Role not found: " + roleId));
        return userRoleRepository.findByRoleId(roleId).stream().map(this::toResult).toList();
    }

    @Override
    public void remove(Long tenantUserId, Long assignmentId) {
        userRoleRepository.findById(assignmentId)
                .filter(a -> a.getTenantUserId().equals(tenantUserId))
                .orElseThrow(() -> new OrgNotFoundException("Role assignment not found: " + assignmentId));
        userRoleRepository.deleteById(assignmentId);
    }

    private UserRoleAssignmentResult toResult(TenantUserRole tur) {
        return new UserRoleAssignmentResult(
                tur.getId(), tur.getTenantUserId(), tur.getRoleId(),
                tur.getDepartmentId(), tur.getCreatedAt()
        );
    }
}
