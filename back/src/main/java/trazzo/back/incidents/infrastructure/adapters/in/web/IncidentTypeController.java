package trazzo.back.incidents.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trazzo.back.incidents.application.dto.command.CreateIncidentTypeCommand;
import trazzo.back.incidents.application.dto.command.PatchIncidentTypeCommand;
import trazzo.back.incidents.application.port.in.IncidentTypeUseCase;
import trazzo.back.incidents.infrastructure.adapters.in.web.dto.CreateIncidentTypeRequest;
import trazzo.back.incidents.infrastructure.adapters.in.web.dto.IncidentTypeListResponse;
import trazzo.back.incidents.infrastructure.adapters.in.web.dto.IncidentTypeResponse;
import trazzo.back.incidents.infrastructure.adapters.in.web.dto.PatchIncidentTypeRequest;

@RestController
@RequestMapping("/incidentes/tipos")
@RequiredArgsConstructor
public class IncidentTypeController {

    private final IncidentTypeUseCase useCase;

    @GetMapping
    public ResponseEntity<IncidentTypeListResponse> list(
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var result = useCase.findAll(activo, page, size);
        return ResponseEntity.ok(IncidentTypeListResponse.from(result));
    }

    @PostMapping
    public ResponseEntity<IncidentTypeResponse> create(@Valid @RequestBody CreateIncidentTypeRequest request) {
        var command = new CreateIncidentTypeCommand(request.nombre(), request.descripcion());
        var result = useCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(IncidentTypeResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentTypeResponse> getById(@PathVariable String id) {
        return useCase.findById(id)
                .map(result -> ResponseEntity.ok(IncidentTypeResponse.from(result)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<IncidentTypeResponse> patch(
            @PathVariable String id,
            @Valid @RequestBody PatchIncidentTypeRequest request
    ) {
        var command = new PatchIncidentTypeCommand(request.nombre(), request.descripcion(), request.activo());
        var result = useCase.patch(id, command);
        return ResponseEntity.ok(IncidentTypeResponse.from(result));
    }
}
