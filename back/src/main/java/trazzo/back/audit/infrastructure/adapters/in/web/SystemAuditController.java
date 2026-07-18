package trazzo.back.audit.infrastructure.adapters.in.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import trazzo.back.audit.application.port.in.SystemAuditUseCase;
import trazzo.back.audit.infrastructure.adapters.in.web.dto.SystemAuditListResponse;
import trazzo.back.audit.infrastructure.adapters.in.web.dto.SystemAuditResponse;

@RestController
@RequestMapping("/audit/system-audit")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('monitoreo-sistema.auditoria-acciones')")
public class SystemAuditController {

    private final SystemAuditUseCase systemAuditUseCase;

    @GetMapping
    public ResponseEntity<SystemAuditListResponse> findAll(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String entity,
            @RequestParam(name = "fecha_desde", required = false) String fechaDesde,
            @RequestParam(name = "fecha_hasta", required = false) String fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        var result = systemAuditUseCase.findAll(searchTerm, module, entity, fechaDesde, fechaHasta, page, size, sort);
        return ResponseEntity.ok(SystemAuditListResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SystemAuditResponse> findById(@PathVariable Long id) {
        var result = systemAuditUseCase.findById(id);
        return ResponseEntity.ok(SystemAuditResponse.from(result));
    }
}
