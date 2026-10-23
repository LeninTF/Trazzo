package trazzo.back.corehr.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trazzo.back.corehr.application.dto.command.CreateUserScheduleCommand;
import trazzo.back.corehr.application.port.in.UserScheduleUseCase;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.CreateUserScheduleRequest;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.UserScheduleListResponse;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.UserScheduleResponse;

@RestController
@RequestMapping("/corehr/user-schedules")
@RequiredArgsConstructor
public class UserScheduleController {

    private final UserScheduleUseCase userScheduleUseCase;

    @GetMapping
    @PreAuthorize("@authz.canViewSchedules()")
    public ResponseEntity<UserScheduleListResponse> list(
            @RequestParam(name = "tenant_user_id", required = false) Long tenantUserId,
            @RequestParam(name = "schedule_id", required = false) Long scheduleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var result = userScheduleUseCase.findAll(tenantUserId, scheduleId, page, size);
        return ResponseEntity.ok(UserScheduleListResponse.from(result));
    }

    @PostMapping
    @PreAuthorize("@authz.any('gestion-horarios.asignacion-masiva')")
    public ResponseEntity<UserScheduleResponse> create(@Valid @RequestBody CreateUserScheduleRequest request) {
        var command = new CreateUserScheduleCommand(request.tenantUserId(), request.scheduleId(),
                request.description(), request.entryTime(), request.departureTime());
        var result = userScheduleUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserScheduleResponse.from(result));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@authz.any('gestion-horarios.asignacion-masiva')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userScheduleUseCase.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
