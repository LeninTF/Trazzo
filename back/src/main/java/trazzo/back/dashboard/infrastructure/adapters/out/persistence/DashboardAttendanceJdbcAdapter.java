package trazzo.back.dashboard.infrastructure.adapters.out.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import trazzo.back.dashboard.application.port.out.DashboardAttendancePort;

import java.time.LocalDate;
import java.util.List;

public class DashboardAttendanceJdbcAdapter implements DashboardAttendancePort {

    private final JdbcTemplate jdbcTemplate;

    public DashboardAttendanceJdbcAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public long countByStateAndDateRange(String state, LocalDate desde, LocalDate hasta) {
        Long result = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendances WHERE state = CAST(? AS attendance_status_enum) AND attendance_date BETWEEN ? AND ?",
                Long.class, state, desde, hasta);
        return result != null ? result : 0L;
    }

    @Override
    public long countTotalByDateRange(LocalDate desde, LocalDate hasta) {
        Long result = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendances WHERE attendance_date BETWEEN ? AND ?",
                Long.class, desde, hasta);
        return result != null ? result : 0L;
    }

    @Override
    public long countPunctualByYear(int year) {
        Long result = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendances WHERE state = 'PUNTUAL' AND EXTRACT(YEAR FROM attendance_date) = ?",
                Long.class, year);
        return result != null ? result : 0L;
    }

    @Override
    public long countTotalByYear(int year) {
        Long result = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendances WHERE EXTRACT(YEAR FROM attendance_date) = ?",
                Long.class, year);
        return result != null ? result : 0L;
    }

    @Override
    public List<RolePunctualityProjection> getPunctualityByRole(LocalDate desde, LocalDate hasta) {
        return jdbcTemplate.query("""
                SELECT r.name AS role_name,
                       COUNT(a.id) AS total,
                       COUNT(a.id) FILTER (WHERE a.state = 'PUNTUAL') AS punctual
                FROM attendances a
                JOIN tenant_user tu ON tu.id = a.tenant_user_id
                JOIN tenant_user_role tur ON tur.tenant_user_id = tu.id
                JOIN role r ON r.id = tur.role_id
                WHERE a.attendance_date BETWEEN ? AND ?
                  AND tu.deleted_at IS NULL
                GROUP BY r.name
                """,
                (rs, rowNum) -> new RolePunctualityProjection(
                        rs.getString("role_name"),
                        rs.getLong("total"),
                        rs.getLong("punctual")),
                desde, hasta);
    }

    @Override
    public long countActiveUsers() {
        Long result = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tenant_user WHERE deleted_at IS NULL",
                Long.class);
        return result != null ? result : 0L;
    }

    @Override
    public List<Long> getUserIdsWithoutAttendance(LocalDate date) {
        return jdbcTemplate.queryForList("""
                SELECT tu.id FROM tenant_user tu
                WHERE tu.deleted_at IS NULL
                  AND tu.id NOT IN (
                    SELECT a.tenant_user_id FROM attendances a
                    WHERE a.attendance_date = ?
                  )
                """,
                Long.class, date);
    }
}
