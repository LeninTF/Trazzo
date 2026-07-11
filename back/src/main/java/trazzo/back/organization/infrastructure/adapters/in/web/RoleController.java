package trazzo.back.organization.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trazzo.back.organization.application.dto.command.AssignPermissionToRoleCommand;
import trazzo.back.organization.application.dto.command.CreateRoleCommand;
import trazzo.back.organization.application.dto.command.UpdateRoleCommand;
import trazzo.back.organization.application.dto.result.RolePermissionResult;
import trazzo.back.organization.application.dto.result.RoleResult;
import trazzo.back.organization.application.dto.result.UserRoleAssignmentResult;
import trazzo.back.organization.application.port.in.RolePermissionsUseCase;
import trazzo.back.organization.application.port.in.RoleUseCase;
import trazzo.back.organization.application.port.in.UserRoleUseCase;
import trazzo.back.organization.application.dto.result.PaginatedResult;
import trazzo.back.organization.infrastructure.adapters.in.web.dto.AssignPermissionRequest;
import trazzo.back.organization.infrastructure.adapters.in.web.dto.RoleRequest;

import java.util.List;

@RestController
@RequestMapping("/org/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleUseCase roleUseCase;
    private final RolePermissionsUseCase rolePermissionsUseCase;
    private final UserRoleUseCase userRoleUseCase;

    @GetMapping
    public ResponseEntity<PaginatedResult<RoleResult>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return ResponseEntity.ok(roleUseCase.findAll(search, page, size, sort));
    }

    @PostMapping
    public ResponseEntity<RoleResult> create(@Valid @RequestBody RoleRequest request) {
        var result = roleUseCase.create(new CreateRoleCommand(request.name(), request.description()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{roleId}")
    public ResponseEntity<RoleResult> getById(@PathVariable String roleId) {
        return roleUseCase.findById(roleId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{roleId}")
    public ResponseEntity<RoleResult> update(
            @PathVariable String roleId,
            @Valid @RequestBody RoleRequest request
    ) {
        return ResponseEntity.ok(roleUseCase.update(roleId, new UpdateRoleCommand(request.name(), request.description())));
    }

    @DeleteMapping("/{roleId}")
    public ResponseEntity<Void> delete(@PathVariable String roleId) {
        roleUseCase.delete(roleId);
        return ResponseEntity.noContent().build();
    }

    // ── Permissions sub-resource ─────────────────────────────────────────────

    @GetMapping("/{roleId}/permissions")
    public ResponseEntity<List<RolePermissionResult>> listPermissions(@PathVariable String roleId) {
        return ResponseEntity.ok(rolePermissionsUseCase.findByRoleId(roleId));
    }

    @PostMapping("/{roleId}/permissions")
    public ResponseEntity<RolePermissionResult> assignPermission(
            @PathVariable String roleId,
            @Valid @RequestBody AssignPermissionRequest request
    ) {
        var result = rolePermissionsUseCase.assign(roleId, new AssignPermissionToRoleCommand(request.permissionId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @DeleteMapping("/{roleId}/permissions/{permissionId}")
    public ResponseEntity<Void> removePermission(
            @PathVariable String roleId,
            @PathVariable String permissionId
    ) {
        rolePermissionsUseCase.remove(roleId, permissionId);
        return ResponseEntity.noContent().build();
    }

    // ── Users sub-resource ───────────────────────────────────────────────────

    @GetMapping("/{roleId}/users")
    public ResponseEntity<List<UserRoleAssignmentResult>> listUsers(@PathVariable String roleId) {
        return ResponseEntity.ok(userRoleUseCase.findByRoleId(roleId));
    }
}
