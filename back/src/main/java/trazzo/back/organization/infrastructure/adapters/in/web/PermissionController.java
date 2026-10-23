package trazzo.back.organization.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trazzo.back.organization.application.dto.command.CreatePermissionCommand;
import trazzo.back.organization.application.dto.command.UpdatePermissionCommand;
import trazzo.back.organization.application.dto.result.PermissionResult;
import trazzo.back.organization.application.dto.result.PaginatedResult;
import trazzo.back.organization.application.port.in.PermissionUseCase;
import trazzo.back.organization.infrastructure.adapters.in.web.dto.PermissionRequest;

@RestController
@RequestMapping("/org/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionUseCase permissionUseCase;

    @GetMapping
    @PreAuthorize("@authz.any('gestion-trabajadores.asignar-roles')")
    public ResponseEntity<PaginatedResult<PermissionResult>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return ResponseEntity.ok(permissionUseCase.findAll(search, page, size, sort));
    }

    @PostMapping
    @PreAuthorize("@authz.admin()")
    public ResponseEntity<PermissionResult> create(@Valid @RequestBody PermissionRequest request) {
        var result = permissionUseCase.create(
                new CreatePermissionCommand(request.name(), request.description(), request.masterFeaturesCode()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{permissionId}")
    @PreAuthorize("@authz.any('gestion-trabajadores.asignar-roles')")
    public ResponseEntity<PermissionResult> getById(@PathVariable String permissionId) {
        return permissionUseCase.findById(permissionId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{permissionId}")
    @PreAuthorize("@authz.admin()")
    public ResponseEntity<PermissionResult> update(
            @PathVariable String permissionId,
            @Valid @RequestBody PermissionRequest request
    ) {
        return ResponseEntity.ok(permissionUseCase.update(permissionId,
                new UpdatePermissionCommand(request.name(), request.description(), request.masterFeaturesCode())));
    }

    @DeleteMapping("/{permissionId}")
    @PreAuthorize("@authz.admin()")
    public ResponseEntity<Void> delete(@PathVariable String permissionId) {
        permissionUseCase.delete(permissionId);
        return ResponseEntity.noContent().build();
    }
}
