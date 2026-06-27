package trazzo.back.reports.infrastructure.adapters.out.persistence.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import trazzo.back.reports.application.ports.out.MonthlyClosureRepositoryPort;
import trazzo.back.reports.domain.model.closure.MonthlyClosure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MonthlyClosureJdbcRepository implements MonthlyClosureRepositoryPort {

    private static final String TABLE = "monthly_closures";
    private static final String COLUMNS = "id, month, year, total_employees, excel_report_url, pdf_report_url, created_by_user_id, created_at";

    private static final String INSERT = "INSERT INTO " + TABLE + " (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_BY_ID = "SELECT " + COLUMNS + " FROM " + TABLE + " WHERE id = ?";
    private static final String SELECT_ALL = "SELECT " + COLUMNS + " FROM " + TABLE + " ORDER BY year DESC, month DESC";
    private static final String SELECT_BY_MONTH_YEAR = "SELECT " + COLUMNS + " FROM " + TABLE + " WHERE month = ? AND year = ?";
    private static final String SELECT_LOCK_BY_MONTH_YEAR = SELECT_BY_MONTH_YEAR + " FOR UPDATE";

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<MonthlyClosure> rowMapper;

    public MonthlyClosureJdbcRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = new MonthlyClosureRowMapper();
    }

    @Override
    public MonthlyClosure save(MonthlyClosure closure) {
        jdbcTemplate.update(INSERT,
                closure.getId(), closure.getMonth(), closure.getYear(),
                closure.getTotalEmployees(), closure.getExcelReportUrl(),
                closure.getPdfReportUrl(), closure.getCreatedByUserId(),
                closure.getCreatedAt());
        return closure;
    }

    @Override
    public Optional<MonthlyClosure> findById(UUID id) {
        List<MonthlyClosure> results = jdbcTemplate.query(SELECT_BY_ID, rowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    @Override
    public List<MonthlyClosure> findAll() {
        return jdbcTemplate.query(SELECT_ALL, rowMapper);
    }

    @Override
    public List<MonthlyClosure> findByMonthAndYear(int month, int year) {
        return jdbcTemplate.query(SELECT_BY_MONTH_YEAR, rowMapper, month, year);
    }

    @Override
    public Optional<MonthlyClosure> findAndLockByMonthAndYear(int month, int year) {
        List<MonthlyClosure> results = jdbcTemplate.query(SELECT_LOCK_BY_MONTH_YEAR, rowMapper, month, year);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
    }

    private static class MonthlyClosureRowMapper implements RowMapper<MonthlyClosure> {
        @Override
        public MonthlyClosure mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new MonthlyClosure(
                    rs.getObject("id", UUID.class),
                    rs.getInt("month"),
                    rs.getInt("year"),
                    rs.getInt("total_employees"),
                    rs.getString("excel_report_url"),
                    rs.getString("pdf_report_url"),
                    rs.getString("created_by_user_id"),
                    rs.getObject("created_at", LocalDateTime.class));
        }
    }
}
