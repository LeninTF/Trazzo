package trazzo.back.audit.application.usecase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import trazzo.back.audit.application.dto.result.AuditMetricsResult;
import trazzo.back.audit.application.port.in.AuditMetricsUseCase;

import java.time.Clock;
import java.time.LocalDateTime;

public class AuditMetricsService implements AuditMetricsUseCase {

    private static final Logger log = LoggerFactory.getLogger(AuditMetricsService.class);

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public AuditMetricsService(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    public AuditMetricsResult getMetrics() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime thirtyDaysAgo = now.minusDays(30);
        LocalDateTime sixtyDaysAgo = now.minusDays(60);

        Long totalEventos = safeCount("SELECT COUNT(*) FROM audit");
        Long recentCount = safeCount("SELECT COUNT(*) FROM audit WHERE created_at >= ?", thirtyDaysAgo);
        Long previousCount = safeCount("SELECT COUNT(*) FROM audit WHERE created_at >= ? AND created_at < ?",
                sixtyDaysAgo, thirtyDaysAgo);
        Long errores = safeCount("SELECT COUNT(*) FROM audit WHERE action = 'DELETE'");
        Long sesionesActivas = safeCount("SELECT COUNT(*) FROM sesion WHERE state = TRUE");

        double crecimiento = 0.0;
        if (previousCount != null && previousCount > 0 && recentCount != null) {
            crecimiento = ((double) (recentCount - previousCount) / previousCount) * 100;
        }

        double porcentajeSesiones = 0.0;
        if (totalEventos != null && totalEventos > 0 && sesionesActivas != null) {
            porcentajeSesiones = ((double) sesionesActivas / totalEventos) * 100;
        }

        return new AuditMetricsResult(
                totalEventos != null ? totalEventos : 0,
                errores != null ? errores : 0,
                sesionesActivas != null ? sesionesActivas : 0,
                Math.round(crecimiento * 10.0) / 10.0,
                Math.round(porcentajeSesiones * 10.0) / 10.0
        );
    }

    private Long safeCount(String sql, Object... args) {
        try {
            Long result = args.length == 0
                    ? jdbcTemplate.queryForObject(sql, Long.class)
                    : jdbcTemplate.queryForObject(sql, Long.class, args);
            return result != null ? result : 0L;
        } catch (DataAccessException e) {
            log.warn("Failed to execute metric query: {}", sql, e);
            return 0L;
        }
    }
}
