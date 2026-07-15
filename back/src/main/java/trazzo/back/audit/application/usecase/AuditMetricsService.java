package trazzo.back.audit.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import trazzo.back.audit.application.dto.result.AuditMetricsResult;
import trazzo.back.audit.application.port.in.AuditMetricsUseCase;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class AuditMetricsService implements AuditMetricsUseCase {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public AuditMetricsResult getMetrics() {
        Long totalEventos = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit", Long.class);

        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        LocalDateTime sixtyDaysAgo = LocalDateTime.now().minusDays(60);

        Long recentCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit WHERE created_at >= ?", Long.class, thirtyDaysAgo);

        Long previousCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit WHERE created_at >= ? AND created_at < ?",
                Long.class, sixtyDaysAgo, thirtyDaysAgo);

        double crecimiento = 0.0;
        if (previousCount != null && previousCount > 0 && recentCount != null) {
            crecimiento = ((double) (recentCount - previousCount) / previousCount) * 100;
        }

        return new AuditMetricsResult(
                totalEventos != null ? totalEventos : 0,
                0,
                0,
                Math.round(crecimiento * 10.0) / 10.0,
                0.0
        );
    }
}
