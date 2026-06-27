package trazzo.back.corehr.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trazzo.back.corehr.application.dto.command.PatchAttendanceCommand;
import trazzo.back.corehr.application.port.in.AttendanceUseCase;
import trazzo.back.corehr.domain.model.AttendanceState;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.AttendanceListResponse;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.AttendanceResponse;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.PatchAttendanceRequest;

import java.time.LocalDate;

@RestController
@RequestMapping("/corehr/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceUseCase attendanceUseCase;

    @GetMapping
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
        var result = attendanceUseCase.findAll(scope, branchId, areaId, departamentoId,
                dateFrom, dateTo, state, tenantUserId, page, size, sort);
        return ResponseEntity.ok(AttendanceListResponse.from(result, scope));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttendanceResponse> getById(@PathVariable String id) {
        return attendanceUseCase.findById(id)
                .map(result -> ResponseEntity.ok(AttendanceResponse.from(result)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<AttendanceResponse> correct(
            @PathVariable String id,
            @Valid @RequestBody PatchAttendanceRequest request
    ) {
        var command = new PatchAttendanceCommand(request.checkIn(), request.checkOut(),
                request.state(), request.minutesLate());
        var result = attendanceUseCase.correct(id, command);
        return ResponseEntity.ok(AttendanceResponse.from(result));
    }
}
