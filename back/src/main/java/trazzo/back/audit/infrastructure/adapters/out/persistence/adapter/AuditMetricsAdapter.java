package trazzo.back.audit.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import trazzo.back.audit.application.port.out.AuditMetricsPort;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AuditMetricsAdapter implements AuditMetricsPort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public long countAll() {
        return safeCount("SELECT COUNT(*) FROM audit");
    }

    @Override
    public long countSince(LocalDateTime since) {
        return safeCount("SELECT COUNT(*) FROM audit WHERE created_at >= ?", since);
    }

    @Override
    public long countBetween(LocalDateTime from, LocalDateTime to) {
        return safeCount("SELECT COUNT(*) FROM audit WHERE created_at >= ? AND created_at < ?", from, to);
    }

    @Override
    public long countByAction(String action) {
        return safeCount("SELECT COUNT(*) FROM audit WHERE action = ?", action);
    }

    @Override
    public long countActiveSessions() {
        return safeCount("SELECT COUNT(*) FROM sesion WHERE state = TRUE");
    }

    private long safeCount(String sql, Object... args) {
        try {
            Long result = args.length == 0
                    ? jdbcTemplate.queryForObject(sql, Long.class)
                    : jdbcTemplate.queryForObject(sql, Long.class, args);
            return result != null ? result : 0L;
        } catch (DataAccessException e) {
            return 0L;
        }
    }
}
