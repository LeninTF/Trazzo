package trazzo.back.organization.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trazzo.back.organization.application.dto.command.CreateBranchCommand;
import trazzo.back.organization.application.dto.command.UpdateBranchCommand;
import trazzo.back.organization.application.dto.result.BranchResult;
import trazzo.back.organization.application.dto.result.PaginatedResult;
import trazzo.back.organization.application.port.in.BranchUseCase;
import trazzo.back.organization.infrastructure.adapters.in.web.dto.BranchRequest;
import trazzo.back.organization.infrastructure.adapters.in.web.dto.UpdateHierarchyRequest;

@RestController
@RequestMapping("/org/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchUseCase branchUseCase;

    @GetMapping
    public ResponseEntity<PaginatedResult<BranchResult>> list(
            @RequestParam(required = false) Boolean state,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        return ResponseEntity.ok(branchUseCase.findAll(state, search, page, size, sort));
    }

    @PostMapping
    public ResponseEntity<BranchResult> create(@Valid @RequestBody BranchRequest request) {
        var result = branchUseCase.create(new CreateBranchCommand(request.name(), request.description()));
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BranchResult> getById(@PathVariable Long id) {
        return branchUseCase.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<BranchResult> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateHierarchyRequest request
    ) {
        return ResponseEntity.ok(branchUseCase.update(id, new UpdateBranchCommand(request.name(), request.description())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        branchUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
