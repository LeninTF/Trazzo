package trazzo.back.organization.application.port.in;

import trazzo.back.organization.application.dto.command.CreateRoleCommand;
import trazzo.back.organization.application.dto.command.UpdateRoleCommand;
import trazzo.back.organization.application.dto.result.PaginatedResult;
import trazzo.back.organization.application.dto.result.RoleResult;

import java.util.Optional;

public interface RoleUseCase {
    RoleResult create(CreateRoleCommand command);
    Optional<RoleResult> findById(String id);
    PaginatedResult<RoleResult> findAll(String search, int page, int size, String sort);
    RoleResult update(String id, UpdateRoleCommand command);
    void delete(String id);
}
