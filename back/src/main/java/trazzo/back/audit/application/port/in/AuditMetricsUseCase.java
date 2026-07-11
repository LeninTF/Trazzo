package trazzo.back.audit.application.port.in;

import trazzo.back.audit.application.dto.result.AuditMetricsResult;

public interface AuditMetricsUseCase {
    AuditMetricsResult getMetrics();
}
