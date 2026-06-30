package trazzo.back.organization.application.port.in;

import trazzo.back.organization.application.dto.command.AssignRoleToUserCommand;
import trazzo.back.organization.application.dto.result.UserRoleAssignmentResult;

import java.util.List;

public interface UserRoleUseCase {
    UserRoleAssignmentResult assign(Long tenantUserId, AssignRoleToUserCommand command);
    List<UserRoleAssignmentResult> findByTenantUserId(Long tenantUserId);
    List<UserRoleAssignmentResult> findByRoleId(String roleId);
    void remove(Long tenantUserId, Long assignmentId);
}
