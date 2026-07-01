package trazzo.back.corehr.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trazzo.back.corehr.application.dto.command.CreateToleranciaCommand;
import trazzo.back.corehr.application.dto.command.PatchToleranciaCommand;
import trazzo.back.corehr.application.port.in.ToleranciaUseCase;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.CreateToleranciaRequest;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.PatchToleranciaRequest;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.ToleranciaListResponse;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.ToleranciaResponse;

@RestController
@RequestMapping("/corehr/schedules/{scheduleId}/tolerancias")
@RequiredArgsConstructor
public class ToleranciaController {

    private final ToleranciaUseCase toleranciaUseCase;

    @GetMapping
    public ResponseEntity<ToleranciaListResponse> listBySchedule(
            @PathVariable Long scheduleId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var result = toleranciaUseCase.findAllByScheduleId(scheduleId, page, size);
        return ResponseEntity.ok(ToleranciaListResponse.from(result));
    }

    @PostMapping
    public ResponseEntity<ToleranciaResponse> create(
            @PathVariable Long scheduleId,
            @Valid @RequestBody CreateToleranciaRequest request
    ) {
        var command = new CreateToleranciaCommand(request.name(), request.type(),
                request.minutes(), request.description());
        var result = toleranciaUseCase.create(scheduleId, command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ToleranciaResponse.from(result));
    }

    @PatchMapping("/{toleranciaId}")
    public ResponseEntity<ToleranciaResponse> patch(
            @PathVariable Long scheduleId,
            @PathVariable Long toleranciaId,
            @Valid @RequestBody PatchToleranciaRequest request
    ) {
        var command = new PatchToleranciaCommand(request.name(), request.minutes(),
                request.description(), request.activo());
        var result = toleranciaUseCase.patch(scheduleId, toleranciaId, command);
        return ResponseEntity.ok(ToleranciaResponse.from(result));
    }

    @DeleteMapping("/{toleranciaId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long scheduleId,
            @PathVariable Long toleranciaId
    ) {
        toleranciaUseCase.deleteById(scheduleId, toleranciaId);
        return ResponseEntity.noContent().build();
    }
}
