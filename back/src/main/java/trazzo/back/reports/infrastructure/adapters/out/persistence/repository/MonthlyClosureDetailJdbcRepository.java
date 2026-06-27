package trazzo.back.reports.infrastructure.adapters.out.persistence.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import trazzo.back.reports.application.ports.out.MonthlyClosureDetailRepositoryPort;
import trazzo.back.reports.domain.model.closure.MonthlyClosureDetail;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MonthlyClosureDetailJdbcRepository implements MonthlyClosureDetailRepositoryPort {

    private static final String TABLE = "monthly_closure_details";
    private static final String COLUMNS = "id, month_closure_id, tenant_user_id, tenant_user_full_name, "
            + "tenant_user_document, department_name, role_name, total_worked_hours, "
            + "total_tardiness_minutes, total_absences, total_overtime_hours, created_at";

    private static final String INSERT = "INSERT INTO " + TABLE + " (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
            + "ON CONFLICT (id) DO NOTHING";
    private static final String SELECT_BY_ID = "SELECT " + COLUMNS + " FROM " + TABLE + " WHERE id = ?";
    private static final String SELECT_BY_CLOSURE_ID = "SELECT " + COLUMNS + " FROM " + TABLE + " WHERE month_closure_id = ?";

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<MonthlyClosureDetail> rowMapper;

    public MonthlyClosureDetailJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = new MonthlyClosureDetailRowMapper();
    }

    @Override
    public MonthlyClosureDetail save(MonthlyClosureDetail detail) {
        jdbcTemplate.update(INSERT,
                detail.getId(), detail.getMonthClosureId(),
                detail.getTenantUserId(), detail.getTenantUserFullName(),
                detail.getTenantUserDocument(), detail.getDepartmentName(),
                detail.getRoleName(), detail.getTotalWorkedHours(),
                detail.getTotalTardinessMinutes(), detail.getTotalAbsences(),
                detail.getTotalOvertimeHours(), detail.getCreatedAt());
        return detail;
    }

    @Override
    public List<MonthlyClosureDetail> saveAll(List<MonthlyClosureDetail> details) {
        for (MonthlyClosureDetail detail : details) {
            save(detail);
        }
        return details;
    }

    @Override
    public Optional<MonthlyClosureDetail> findById(UUID id) {
        List<MonthlyClosureDetail> results = jdbcTemplate.query(SELECT_BY_ID, rowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public List<MonthlyClosureDetail> findByMonthlyClosureId(UUID monthlyClosureId) {
        return jdbcTemplate.query(SELECT_BY_CLOSURE_ID, rowMapper, monthlyClosureId);
    }

    private static class MonthlyClosureDetailRowMapper implements RowMapper<MonthlyClosureDetail> {
        @Override
        public MonthlyClosureDetail mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new MonthlyClosureDetail(
                    rs.getObject("id", UUID.class),
                    rs.getObject("month_closure_id", UUID.class),
                    rs.getString("tenant_user_id"),
                    rs.getString("tenant_user_full_name"),
                    rs.getString("tenant_user_document"),
                    rs.getString("department_name"),
                    rs.getString("role_name"),
                    rs.getDouble("total_worked_hours"),
                    rs.getDouble("total_tardiness_minutes"),
                    rs.getInt("total_absences"),
                    rs.getDouble("total_overtime_hours"),
                    rs.getObject("created_at", LocalDateTime.class));
        }
    }
}
