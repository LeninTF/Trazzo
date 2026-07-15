package trazzo.back.audit.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.audit.application.dto.result.AuditMetricsResult;
import trazzo.back.audit.application.port.in.AuditMetricsUseCase;
import trazzo.back.audit.application.port.out.AuditRepositoryPort;

import java.time.LocalDateTime;
import java.time.ZoneId;

@RequiredArgsConstructor
public class AuditMetricsService implements AuditMetricsUseCase {

    private final AuditRepositoryPort auditRepository;

    @Override
    public AuditMetricsResult getMetrics() {
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());

        long totalEventos = auditRepository.count(null, null, null, null, null);

        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime startOfYesterday = now.toLocalDate().minusDays(1).atStartOfDay();
        long todayCount = auditRepository.count(null, null, null, startOfToday, now);
        long yesterdayCount = auditRepository.count(null, null, null, startOfYesterday, startOfToday);
        double crecimiento = yesterdayCount > 0
                ? ((double) (todayCount - yesterdayCount) / yesterdayCount) * 100.0
                : 0.0;

        return new AuditMetricsResult(totalEventos, 0, 0, crecimiento, 0.0);
    }
}
