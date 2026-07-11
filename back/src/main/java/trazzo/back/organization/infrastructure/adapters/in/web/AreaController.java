package trazzo.back.organization.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trazzo.back.organization.application.dto.command.CreateAreaCommand;
import trazzo.back.organization.application.dto.command.UpdateAreaCommand;
import trazzo.back.organization.application.dto.result.AreaResult;
import trazzo.back.organization.application.dto.result.PaginatedResult;
import trazzo.back.organization.application.port.in.AreaUseCase;
import trazzo.back.organization.infrastructure.adapters.in.web.dto.AreaRequest;
import trazzo.back.organization.infrastructure.adapters.in.web.dto.UpdateHierarchyRequest;

@RestController
@RequestMapping("/org/areas")
@RequiredArgsConstructor
public class AreaController {

    private final AreaUseCase areaUseCase;

    @GetMapping
    public ResponseEntity<PaginatedResult<AreaResult>> list(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Boolean state,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return ResponseEntity.ok(areaUseCase.findAll(branchId, state, search, page, size, sort));
    }

    @PostMapping
    public ResponseEntity<AreaResult> create(@Valid @RequestBody AreaRequest request) {
        var result = areaUseCase.create(new CreateAreaCommand(request.branchId(), request.name(), request.description()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AreaResult> getById(@PathVariable Long id) {
        return areaUseCase.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<AreaResult> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateHierarchyRequest request
    ) {
        return ResponseEntity.ok(areaUseCase.update(id, new UpdateAreaCommand(request.name(), request.description())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        areaUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
