package trazzo.back.corehr.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trazzo.back.corehr.application.dto.command.CreateShiftCommand;
import trazzo.back.corehr.application.dto.command.PatchShiftCommand;
import trazzo.back.corehr.application.port.in.ShiftUseCase;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.CreateShiftRequest;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.PatchShiftRequest;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.ShiftListResponse;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.ShiftResponse;

@RestController
@RequestMapping("/corehr/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftUseCase shiftUseCase;

    @GetMapping
    public ResponseEntity<ShiftListResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        var result = shiftUseCase.findAll(search, page, size, sort);
        return ResponseEntity.ok(ShiftListResponse.from(result));
    }

    @PostMapping
    public ResponseEntity<ShiftResponse> create(@Valid @RequestBody CreateShiftRequest request) {
        var command = new CreateShiftCommand(request.name(), request.description());
        var result = shiftUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(ShiftResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShiftResponse> getById(@PathVariable Long id) {
        return shiftUseCase.findById(id)
                .map(result -> ResponseEntity.ok(ShiftResponse.from(result)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ShiftResponse> patch(
            @PathVariable Long id,
            @Valid @RequestBody PatchShiftRequest request
    ) {
        var command = new PatchShiftCommand(request.name(), request.description());
        var result = shiftUseCase.patch(id, command);
        return ResponseEntity.ok(ShiftResponse.from(result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        shiftUseCase.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
