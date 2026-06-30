package trazzo.back.organization.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.organization.application.dto.command.CreateDepartmentCommand;
import trazzo.back.organization.application.dto.command.UpdateDepartmentCommand;
import trazzo.back.organization.application.dto.result.DepartmentResult;
import trazzo.back.organization.application.dto.result.PaginatedResult;
import trazzo.back.organization.application.port.in.DepartmentUseCase;
import trazzo.back.organization.application.port.out.AreaRepositoryPort;
import trazzo.back.organization.application.port.out.DepartmentRepositoryPort;
import trazzo.back.organization.domain.exception.DuplicateOrgNameException;
import trazzo.back.organization.domain.exception.OrgNotFoundException;
import trazzo.back.organization.domain.model.business.Department;

import java.util.Optional;

@RequiredArgsConstructor
public class DepartmentService implements DepartmentUseCase {

    private final DepartmentRepositoryPort departmentRepository;
    private final AreaRepositoryPort areaRepository;

    @Override
    public DepartmentResult create(CreateDepartmentCommand command) {
        var area = areaRepository.findById(command.areaId())
                .orElseThrow(() -> new OrgNotFoundException("Area not found: " + command.areaId()));
        if (!area.isState()) {
            throw new OrgNotFoundException("Area not found: " + command.areaId());
        }
        if (departmentRepository.existsByAreaIdAndName(command.areaId(), command.name())) {
            throw new DuplicateOrgNameException("Department '" + command.name() + "' already exists in this area");
        }
        var department = Department.create(command.areaId(), command.name(), command.description());
        return toResult(departmentRepository.save(department));
    }

    @Override
    public Optional<DepartmentResult> findById(Long id) {
        return departmentRepository.findById(id).map(this::toResult);
    }

    @Override
    public PaginatedResult<DepartmentResult> findAll(Long areaId, Boolean state, String search, int page, int size, String sort) {
        var items = departmentRepository.findAll(areaId, state, search, page, size, sort);
        var total = departmentRepository.count(areaId, state, search);
        return PaginatedResult.of(items.stream().map(this::toResult).toList(), page, size, total);
    }

    @Override
    public DepartmentResult update(Long id, UpdateDepartmentCommand command) {
        var department = departmentRepository.findById(id)
                .orElseThrow(() -> new OrgNotFoundException("Department not found: " + id));
        if (departmentRepository.existsByAreaIdAndNameAndIdNot(department.getAreaId(), command.name(), id)) {
            throw new DuplicateOrgNameException("Department '" + command.name() + "' already exists in this area");
        }
        department.update(command.name(), command.description());
        return toResult(departmentRepository.save(department));
    }

    @Override
    public void delete(Long id) {
        var department = departmentRepository.findById(id)
                .orElseThrow(() -> new OrgNotFoundException("Department not found: " + id));
        department.softDelete();
        departmentRepository.save(department);
    }

    private DepartmentResult toResult(Department department) {
        return new DepartmentResult(
                department.getId(), department.getAreaId(), department.getName(), department.getDescription(),
                department.isState(), department.getCreatedAt(), department.getUpdatedAt(), department.getDeletedAt()
        );
    }
}
