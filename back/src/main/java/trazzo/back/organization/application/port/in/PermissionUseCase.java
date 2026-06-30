package trazzo.back.organization.application.port.in;

import trazzo.back.organization.application.dto.command.CreatePermissionCommand;
import trazzo.back.organization.application.dto.command.UpdatePermissionCommand;
import trazzo.back.organization.application.dto.result.PaginatedResult;
import trazzo.back.organization.application.dto.result.PermissionResult;

import java.util.Optional;

public interface PermissionUseCase {
    PermissionResult create(CreatePermissionCommand command);
    Optional<PermissionResult> findById(String id);
    PaginatedResult<PermissionResult> findAll(String search, int page, int size, String sort);
    PermissionResult update(String id, UpdatePermissionCommand command);
    void delete(String id);
}
