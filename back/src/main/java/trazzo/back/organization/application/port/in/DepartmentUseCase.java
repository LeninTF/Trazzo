package trazzo.back.organization.application.port.in;

import trazzo.back.organization.application.dto.command.CreateDepartmentCommand;
import trazzo.back.organization.application.dto.command.UpdateDepartmentCommand;
import trazzo.back.organization.application.dto.result.DepartmentResult;
import trazzo.back.organization.application.dto.result.PaginatedResult;

import java.util.Optional;

public interface DepartmentUseCase {
    DepartmentResult create(CreateDepartmentCommand command);
    Optional<DepartmentResult> findById(Long id);
    PaginatedResult<DepartmentResult> findAll(Long areaId, Boolean state, String search, int page, int size, String sort);
    DepartmentResult update(Long id, UpdateDepartmentCommand command);
    void delete(Long id);
}
