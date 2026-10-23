package trazzo.back.organization.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trazzo.back.organization.application.dto.command.CreateDepartmentCommand;
import trazzo.back.organization.application.dto.command.UpdateDepartmentCommand;
import trazzo.back.organization.application.dto.result.DepartmentResult;
import trazzo.back.organization.application.dto.result.PaginatedResult;
import trazzo.back.organization.application.port.in.DepartmentUseCase;
import trazzo.back.organization.infrastructure.adapters.in.web.dto.DepartmentRequest;
import trazzo.back.organization.infrastructure.adapters.in.web.dto.UpdateHierarchyRequest;

@RestController
@RequestMapping("/org/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentUseCase departmentUseCase;

    @GetMapping
    @PreAuthorize("@authz.canViewStructure()")
    public ResponseEntity<PaginatedResult<DepartmentResult>> list(
            @RequestParam(required = false) Long areaId,
            @RequestParam(required = false) Boolean state,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return ResponseEntity.ok(departmentUseCase.findAll(areaId, state, search, page, size, sort));
    }

    @PostMapping
    @PreAuthorize("@authz.any('estructura-organizacional.crear-editar-areas')")
    public ResponseEntity<DepartmentResult> create(@Valid @RequestBody DepartmentRequest request) {
        var result = departmentUseCase.create(new CreateDepartmentCommand(request.areaId(), request.name(), request.description()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.canViewStructure()")
    public ResponseEntity<DepartmentResult> getById(@PathVariable Long id) {
        return departmentUseCase.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.any('estructura-organizacional.crear-editar-areas')")
    public ResponseEntity<DepartmentResult> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateHierarchyRequest request
    ) {
        return ResponseEntity.ok(departmentUseCase.update(id, new UpdateDepartmentCommand(request.name(), request.description())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.any('estructura-organizacional.crear-editar-areas')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        departmentUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
