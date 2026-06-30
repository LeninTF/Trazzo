package trazzo.back.audit.infrastructure.adapters.in.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trazzo.back.audit.application.port.in.AuditMetricsUseCase;
import trazzo.back.audit.infrastructure.adapters.in.web.dto.AuditMetricsResponse;

@RestController
@RequestMapping("/audit/metrics")
@RequiredArgsConstructor
public class AuditMetricsController {

    private final AuditMetricsUseCase auditMetricsUseCase;

    @GetMapping
    public ResponseEntity<AuditMetricsResponse> getMetrics() {
        var result = auditMetricsUseCase.getMetrics();
        return ResponseEntity.ok(AuditMetricsResponse.from(result));
    }
}
