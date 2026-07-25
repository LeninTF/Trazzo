package trazzo.back.corehr.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import trazzo.back.corehr.application.dto.command.BulkAssignSchedulesCommand;
import trazzo.back.corehr.application.dto.command.CreateUserScheduleCommand;
import trazzo.back.corehr.application.port.in.UserScheduleUseCase;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.BulkAssignUserSchedulesRequest;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.BulkAssignUserSchedulesResponse;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.CreateUserScheduleRequest;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.UserScheduleListResponse;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.UserScheduleResponse;

import java.util.List;

@RestController
@RequestMapping("/corehr/user-schedules")
@RequiredArgsConstructor
public class UserScheduleController {

    private final UserScheduleUseCase userScheduleUseCase;

    @GetMapping
    @PreAuthorize("hasAuthority('gestion-horarios.ver-asignaciones')")
    public ResponseEntity<UserScheduleListResponse> list(
            @RequestParam(name = "tenant_user_id", required = false) Long tenantUserId,
            @RequestParam(name = "schedule_id", required = false) Long scheduleId,
            @RequestParam(name = "shift_id", required = false) Long shiftId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var result = userScheduleUseCase.findAll(tenantUserId, scheduleId, shiftId, page, size);
        return ResponseEntity.ok(UserScheduleListResponse.from(result));
    }

    @GetMapping("/by-user/{tenantUserId}")
    @PreAuthorize("hasAuthority('gestion-horarios.ver-asignaciones')")
    public ResponseEntity<List<UserScheduleResponse>> listByUser(@PathVariable Long tenantUserId) {
        var result = userScheduleUseCase.findByTenantUserId(tenantUserId);
        var dto = result.stream().map(UserScheduleResponse::from).toList();
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('gestion-horarios.asignacion-individual')")
    public ResponseEntity<UserScheduleResponse> create(@Valid @RequestBody CreateUserScheduleRequest request) {
        var command = new CreateUserScheduleCommand(request.tenantUserId(), request.scheduleId(),
                request.description(), request.entryTime(), request.departureTime());
        var result = userScheduleUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserScheduleResponse.from(result));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAuthority('gestion-horarios.asignacion-masiva')")
    public ResponseEntity<BulkAssignUserSchedulesResponse> bulkAssign(
            @Valid @RequestBody BulkAssignUserSchedulesRequest request) {
        var command = new BulkAssignSchedulesCommand(request.tenantUserIds(), request.scheduleId(),
                request.description());
        var result = userScheduleUseCase.bulkAssign(command);
        return ResponseEntity.ok(BulkAssignUserSchedulesResponse.from(result));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('gestion-horarios.asignacion-individual')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userScheduleUseCase.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
