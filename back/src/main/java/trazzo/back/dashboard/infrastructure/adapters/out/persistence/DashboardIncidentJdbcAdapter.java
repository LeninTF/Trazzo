package trazzo.back.dashboard.infrastructure.adapters.out.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import trazzo.back.dashboard.application.port.out.DashboardIncidentPort;

import java.time.LocalDateTime;

public class DashboardIncidentJdbcAdapter implements DashboardIncidentPort {

    private final JdbcTemplate jdbcTemplate;

    public DashboardIncidentJdbcAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long countByDateRange(LocalDateTime desde, LocalDateTime hasta) {
        Long result = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM incidencias WHERE created_at >= ? AND created_at < ?",
                Long.class, desde, hasta);
        return result != null ? result : 0L;
    }
}
