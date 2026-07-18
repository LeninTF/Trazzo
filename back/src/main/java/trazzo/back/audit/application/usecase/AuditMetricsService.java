package trazzo.back.audit.application.usecase;

import org.springframework.jdbc.core.JdbcTemplate;
import trazzo.back.audit.application.dto.result.AuditMetricsResult;
import trazzo.back.audit.application.port.in.AuditMetricsUseCase;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class AuditMetricsService implements AuditMetricsUseCase {

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final ZoneId zone;

    public AuditMetricsService(JdbcTemplate jdbcTemplate, Clock clock, ZoneId zone) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.zone = zone;
    }

    public AuditMetricsService(JdbcTemplate jdbcTemplate, Clock clock) {
        this(jdbcTemplate, clock, ZoneId.of("America/Mexico_City"));
    }

    @Override
    public AuditMetricsResult getMetrics() {
        Long totalEventos = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit", Long.class);

        LocalDateTime thirtyDaysAgo = LocalDateTime.now(clock).atZone(zone).toLocalDateTime().minusDays(30);
        LocalDateTime sixtyDaysAgo = LocalDateTime.now(clock).atZone(zone).toLocalDateTime().minusDays(60);

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
