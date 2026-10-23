package trazzo.back.corehr.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import trazzo.back.corehr.application.dto.command.CreateTenantUserCommand;
import trazzo.back.corehr.application.dto.command.PatchTenantUserCommand;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.SoftDeleteResult;
import trazzo.back.corehr.application.dto.result.TenantUserProfileResult;
import trazzo.back.corehr.application.port.in.TenantUserUseCase;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.*;
import trazzo.back.shared.security.AuthenticatedUser;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class TenantUserController {

    private final TenantUserUseCase tenantUserUseCase;

    @GetMapping
    @PreAuthorize("@authz.canViewStaff()")
    public ResponseEntity<PaginatedResult<TenantUserProfileResult>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return ResponseEntity.ok(tenantUserUseCase.findAll(search, status, page, size, sort));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authz.canViewStaff()")
    public ResponseEntity<TenantUserProfileResult> getById(@PathVariable Long id) {
        return tenantUserUseCase.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("@authz.any('gestion-trabajadores.crear')")
    public ResponseEntity<TenantUserProfileResult> create(@Valid @RequestBody CreateTenantUserRequest request) {
        var command = new CreateTenantUserCommand(
                request.documentType(), request.documentValue(),
                request.name(), request.fatherSurname(), request.motherSurname(),
                request.birthDate(), request.imgUrl(),
                request.email(), request.phone(),
                request.roleId(),
                request.sedeIds(), request.areaIds(), request.departamentoIds()
        );
        var result = tenantUserUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@authz.any('gestion-trabajadores.editar')")
    public ResponseEntity<TenantUserProfileResult> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateTenantUserRequest request
    ) {
        var command = new CreateTenantUserCommand(
                request.documentType(), request.documentValue(),
                request.name(), request.fatherSurname(), request.motherSurname(),
                request.birthDate(), request.imgUrl(),
                request.email(), request.phone(),
                request.roleId(),
                request.sedeIds(), request.areaIds(), request.departamentoIds()
        );
        return ResponseEntity.ok(tenantUserUseCase.update(id, command));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@authz.any('gestion-trabajadores.editar')")
    public ResponseEntity<TenantUserProfileResult> patch(
            @PathVariable Long id,
            @Valid @RequestBody PatchTenantUserRequest request
    ) {
        var command = new PatchTenantUserCommand(
                request.name(), request.fatherSurname(), request.motherSurname(),
                request.birthDate(), request.imgUrl(),
                request.email(), request.phone(),
                request.cargo(), request.estado(), request.roleId(),
                request.sedeIds(), request.areaIds(), request.departamentoIds()
        );
        return ResponseEntity.ok(tenantUserUseCase.patch(id, command));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.any('gestion-trabajadores.eliminar')")
    public ResponseEntity<SoftDeleteResult> delete(@PathVariable Long id) {
        return ResponseEntity.ok(tenantUserUseCase.delete(id));
    }

    @PutMapping("/{id}/rol")
    @PreAuthorize("@authz.any('gestion-trabajadores.asignar-roles')")
    public ResponseEntity<TenantUserProfileResult> assignRole(
            @PathVariable Long id,
            @Valid @RequestBody AssignRoleRequest request
    ) {
        return ResponseEntity.ok(tenantUserUseCase.assignRole(id, request.roleId()));
    }

    // Solo la propia: el caso de uso valida la contraseña actual y para otra cuenta serviría para adivinarla.
    @PatchMapping("/{id}/password")
    @PreAuthorize("@authz.isSelf(#id)")
    public ResponseEntity<Void> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        tenantUserUseCase.changePassword(id, request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TenantUserProfileResult> getMe(
            @AuthenticationPrincipal AuthenticatedUser principal
    ) {
        return tenantUserUseCase.findMe(principal.id().toString())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TenantUserProfileResult> patchMe(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @RequestBody PatchTenantUserRequest request
    ) {
        var result = tenantUserUseCase.patchMe(principal.id().toString(), request.phone(), request.imgUrl());
        return ResponseEntity.ok(result);
    }
}
