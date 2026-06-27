package trazzo.back.corehr.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trazzo.back.corehr.application.dto.command.CreateScheduleCommand;
import trazzo.back.corehr.application.dto.command.PatchScheduleCommand;
import trazzo.back.corehr.application.port.in.ScheduleUseCase;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.CreateScheduleRequest;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.PatchScheduleRequest;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.ScheduleListResponse;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.ScheduleResponse;

@RestController
@RequestMapping("/corehr/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleUseCase scheduleUseCase;

    @GetMapping
    public ResponseEntity<ScheduleListResponse> list(
            @RequestParam(name = "shift_id", required = false) Long shiftId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        var result = scheduleUseCase.findAll(shiftId, page, size, sort);
        return ResponseEntity.ok(ScheduleListResponse.from(result));
    }

    @PostMapping
    public ResponseEntity<ScheduleResponse> create(@Valid @RequestBody CreateScheduleRequest request) {
        var command = new CreateScheduleCommand(request.shiftId(), request.name(),
                request.description(), request.entryTime(), request.departureTime());
        var result = scheduleUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ScheduleResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduleResponse> getById(@PathVariable Long id) {
        return scheduleUseCase.findById(id)
                .map(result -> ResponseEntity.ok(ScheduleResponse.from(result)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ScheduleResponse> patch(
            @PathVariable Long id,
            @Valid @RequestBody PatchScheduleRequest request
    ) {
        var command = new PatchScheduleCommand(request.name(), request.description(),
                request.entryTime(), request.departureTime());
        var result = scheduleUseCase.patch(id, command);
        return ResponseEntity.ok(ScheduleResponse.from(result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scheduleUseCase.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
