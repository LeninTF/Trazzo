package trazzo.back.corehr.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trazzo.back.corehr.application.dto.command.PatchAttendanceCommand;
import trazzo.back.corehr.application.port.in.AttendanceUseCase;
import trazzo.back.corehr.domain.model.AttendanceState;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.AttendanceListResponse;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.AttendanceResponse;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.PatchAttendanceRequest;
import trazzo.back.shared.security.TenantAuthorization;

import java.time.LocalDate;

@RestController
@RequestMapping("/corehr/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private static final String SELF = "SELF";

    private final AttendanceUseCase attendanceUseCase;
    private final TenantAuthorization authz;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendanceListResponse> list(
            @RequestParam(required = false) String scope,
            @RequestParam(name = "branch_id", required = false) Long branchId,
            @RequestParam(name = "area_id", required = false) Long areaId,
            @RequestParam(name = "departamento_id", required = false) Long departamentoId,
            @RequestParam(name = "date_from", required = false) LocalDate dateFrom,
            @RequestParam(name = "date_to", required = false) LocalDate dateTo,
            @RequestParam(required = false) AttendanceState state,
            @RequestParam(name = "tenant_user_id", required = false) Long tenantUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        // Sin VER_ASISTENCIA_TODOS solo se lista la asistencia propia, se pida lo que se pida (AUD-08).
        boolean onlyOwn = SELF.equals(scope) || !authz.canViewAllAttendance();
        String appliedScope = onlyOwn ? SELF : scope;
        Long userFilter = onlyOwn ? currentTenantUserId() : tenantUserId;
        var result = attendanceUseCase.findAll(appliedScope, branchId, areaId, departamentoId,
                dateFrom, dateTo, state, userFilter, page, size, sort);
        return ResponseEntity.ok(AttendanceListResponse.from(result, appliedScope));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendanceResponse> getById(@PathVariable String id) {
        return attendanceUseCase.findById(id)
                .map(result -> {
                    if (!authz.canViewAllAttendance() && !authz.isSelf(result.tenantUserId())) {
                        throw new AccessDeniedException("La asistencia pertenece a otro usuario");
                    }
                    return ResponseEntity.ok(AttendanceResponse.from(result));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@authz.any('control-asistencia.corregir-marcaciones')")
    public ResponseEntity<AttendanceResponse> correct(
            @PathVariable String id,
            @Valid @RequestBody PatchAttendanceRequest request
    ) {
        var command = new PatchAttendanceCommand(request.checkIn(), request.checkOut(),
                request.state(), request.minutesLate());
        var result = attendanceUseCase.correct(id, command);
        return ResponseEntity.ok(AttendanceResponse.from(result));
    }

    private Long currentTenantUserId() {
        return authz.currentTenantUserId()
                .orElseThrow(() -> new AccessDeniedException("No hay usuario de tenant para el usuario autenticado"));
    }
}
