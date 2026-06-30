package trazzo.back.audit.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.audit.application.dto.result.AuditMetricsResult;
import trazzo.back.audit.application.port.in.AuditMetricsUseCase;

@RequiredArgsConstructor
public class AuditMetricsService implements AuditMetricsUseCase {

    @Override
    public AuditMetricsResult getMetrics() {
        return new AuditMetricsResult(0, 0, 0, 0.0, 0.0);
    }
}
