package trazzo.back.corehr.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trazzo.back.corehr.application.dto.command.CreateNonWorkingDayCommand;
import trazzo.back.corehr.application.dto.command.PatchNonWorkingDayCommand;
import trazzo.back.corehr.application.port.in.NonWorkingDayUseCase;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.CreateNonWorkingDayRequest;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.NonWorkingDayListResponse;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.NonWorkingDayResponse;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.PatchNonWorkingDayRequest;

import java.time.LocalDate;

@RestController
@RequestMapping("/corehr/non-working-days")
@RequiredArgsConstructor
public class NonWorkingDayController {

    private final NonWorkingDayUseCase nonWorkingDayUseCase;

    @GetMapping
    public ResponseEntity<NonWorkingDayListResponse> list(
            @RequestParam(name = "date_from", required = false) LocalDate dateFrom,
            @RequestParam(name = "date_to", required = false) LocalDate dateTo,
            @RequestParam(name = "is_recurring", required = false) Boolean isRecurring,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var result = nonWorkingDayUseCase.findAll(dateFrom, dateTo, isRecurring, page, size);
        return ResponseEntity.ok(NonWorkingDayListResponse.from(result));
    }

    @PostMapping
    public ResponseEntity<NonWorkingDayResponse> create(@Valid @RequestBody CreateNonWorkingDayRequest request) {
        var command = new CreateNonWorkingDayCommand(request.date(), request.description(), request.isRecurring());
        var result = nonWorkingDayUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(NonWorkingDayResponse.from(result));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<NonWorkingDayResponse> patch(
            @PathVariable Long id,
            @Valid @RequestBody PatchNonWorkingDayRequest request
    ) {
        var command = new PatchNonWorkingDayCommand(request.date(), request.description(), request.isRecurring());
        var result = nonWorkingDayUseCase.patch(id, command);
        return ResponseEntity.ok(NonWorkingDayResponse.from(result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        nonWorkingDayUseCase.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
