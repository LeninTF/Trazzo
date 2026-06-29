package trazzo.back.corehr.infrastructure.adapters.in.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trazzo.back.corehr.application.dto.command.CreateTenantContactCommand;
import trazzo.back.corehr.application.dto.command.PatchTenantContactCommand;
import trazzo.back.corehr.application.port.in.TenantContactUseCase;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.CreateTenantContactRequest;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.PatchTenantContactRequest;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.TenantContactListResponse;
import trazzo.back.corehr.infrastructure.adapters.in.web.dto.TenantContactResponse;

@RestController
@RequestMapping("/corehr/tenant-contacts")
@RequiredArgsConstructor
public class TenantContactController {

    private final TenantContactUseCase tenantContactUseCase;

    @GetMapping
    public ResponseEntity<TenantContactListResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        var result = tenantContactUseCase.findAll(page, size);
        return ResponseEntity.ok(TenantContactListResponse.from(result));
    }

    @PostMapping
    public ResponseEntity<TenantContactResponse> create(@Valid @RequestBody CreateTenantContactRequest request) {
        var command = new CreateTenantContactCommand(request.tenantUserId(), request.type());
        var result = tenantContactUseCase.create(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(TenantContactResponse.from(result));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TenantContactResponse> patch(
            @PathVariable Long id,
            @Valid @RequestBody PatchTenantContactRequest request
    ) {
        var command = new PatchTenantContactCommand(request.type());
        var result = tenantContactUseCase.patch(id, command);
        return ResponseEntity.ok(TenantContactResponse.from(result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tenantContactUseCase.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
