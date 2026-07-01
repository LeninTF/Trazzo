package trazzo.back.corehr.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trazzo.back.corehr.application.dto.command.CreateTenantUserDepartmentCommand;
import trazzo.back.corehr.application.dto.command.PatchTenantUserDepartmentCommand;
import trazzo.back.corehr.application.port.in.TenantUserDepartmentUseCase;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.CreateTenantUserDepartmentRequest;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.PatchTenantUserDepartmentRequest;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.TenantUserDepartmentListResponse;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.TenantUserDepartmentResponse;

@RestController
@RequestMapping("/corehr/usuarios/{tenantUserId}/departamentos")
@RequiredArgsConstructor
public class TenantUserDepartmentController {

    private final TenantUserDepartmentUseCase tenantUserDepartmentUseCase;

    @GetMapping
    public ResponseEntity<TenantUserDepartmentListResponse> listByUser(@PathVariable Long tenantUserId) {
        var result = tenantUserDepartmentUseCase.findAllByTenantUserId(tenantUserId);
        return ResponseEntity.ok(TenantUserDepartmentListResponse.from(result));
    }

    @PostMapping
    public ResponseEntity<TenantUserDepartmentResponse> create(
            @PathVariable Long tenantUserId,
            @Valid @RequestBody CreateTenantUserDepartmentRequest request
    ) {
        var command = new CreateTenantUserDepartmentCommand(request.departmentId(), request.isPrimary(),
                request.startDate(), request.endDate());
        var result = tenantUserDepartmentUseCase.create(tenantUserId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(TenantUserDepartmentResponse.from(result));
    }

    @PatchMapping("/{departamentoId}")
    public ResponseEntity<TenantUserDepartmentResponse> patch(
            @PathVariable Long tenantUserId,
            @PathVariable Long departamentoId,
            @Valid @RequestBody PatchTenantUserDepartmentRequest request
    ) {
        var command = new PatchTenantUserDepartmentCommand(request.endDate(), request.isPrimary());
        var result = tenantUserDepartmentUseCase.patch(tenantUserId, departamentoId, command);
        return ResponseEntity.ok(TenantUserDepartmentResponse.from(result));
    }
}
