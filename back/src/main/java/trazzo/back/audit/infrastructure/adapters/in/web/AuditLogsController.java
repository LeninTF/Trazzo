package trazzo.back.audit.infrastructure.adapters.in.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import trazzo.back.audit.application.port.in.AuditLogUseCase;
import trazzo.back.audit.domain.model.master.Action;
import trazzo.back.audit.infrastructure.adapters.in.web.dto.AuditLogDetailResponse;
import trazzo.back.audit.infrastructure.adapters.in.web.dto.AuditLogListResponse;

@RestController
@RequestMapping("/audit/logs")
@RequiredArgsConstructor
public class AuditLogsController {

    private final AuditLogUseCase auditLogUseCase;

    @GetMapping
    public ResponseEntity<AuditLogListResponse> findAll(
            @RequestParam(required = false) String searchTerm,
            @RequestParam(name = "tenant_id", required = false) String tenantId,
            @RequestParam(required = false) Action action,
            @RequestParam(required = false) String entity,
            @RequestParam(name = "fecha_desde", required = false) String fechaDesde,
            @RequestParam(name = "fecha_hasta", required = false) String fechaHasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort) {
        var result = auditLogUseCase.findAll(searchTerm, tenantId, action, entity, fechaDesde, fechaHasta, page, size, sort);
        return ResponseEntity.ok(AuditLogListResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AuditLogDetailResponse> findById(@PathVariable String id) {
        var result = auditLogUseCase.findById(id);
        return ResponseEntity.ok(AuditLogDetailResponse.from(result));
    }
}
