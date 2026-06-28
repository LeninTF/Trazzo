package trazzo.back.reports.infrastructure.adapters.out.attendance;

import org.springframework.jdbc.core.JdbcTemplate;
import trazzo.back.reports.application.ports.out.EmployeeAttendanceSummaryPort;

import java.util.List;

public class EmployeeAttendanceSummaryAdapter implements EmployeeAttendanceSummaryPort {

    private final JdbcTemplate jdbcTemplate;

    private static final String QUERY = """
        SELECT
            tu.id,
            COALESCE(p.name || ' ' || p.father_surname || ' ' || p.mother_surname, '') AS tenant_user_full_name,
            COALESCE(p.document_value, '') AS tenant_user_document,
            COALESCE(d.name, '') AS department_name,
            COALESCE(r.name, '') AS role_name,
            COALESCE(SUM(EXTRACT(EPOCH FROM (a.check_out - a.check_in)) / 3600.0), 0) AS total_worked_hours,
            COALESCE(SUM(a.minutes_late), 0) AS total_tardiness_minutes,
            COALESCE(COUNT(*) FILTER (WHERE a.state = 'FALTA'), 0) AS total_absences,
            0 AS total_overtime_hours
        FROM tenant_user tu
        LEFT JOIN users u ON u.id = tu.master_user_id
        LEFT JOIN persons p ON p.id = u.person_id
        LEFT JOIN attendances a ON a.tenant_user_id = tu.id
            AND EXTRACT(MONTH FROM a.attendance_date) = ?
            AND EXTRACT(YEAR FROM a.attendance_date) = ?
        LEFT JOIN LATERAL (
            SELECT tur.department_id, tur.role_id
            FROM tenant_user_role tur
            WHERE tur.tenant_user_id = tu.id
            ORDER BY tur.created_at DESC
            LIMIT 1
        ) tur ON true
        LEFT JOIN department d ON d.id = tur.department_id
        LEFT JOIN role r ON r.id = tur.role_id
        WHERE tu.deleted_at IS NULL
        GROUP BY tu.id, p.name, p.father_surname, p.mother_surname, p.document_value, d.name, r.name
    """;

    public EmployeeAttendanceSummaryAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<EmployeeMonthlySummary> getMonthlySummaries(int month, int year) {
        return jdbcTemplate.query(QUERY,
                (rs, rowNum) -> new EmployeeMonthlySummary(
                        rs.getInt("id"),
                        rs.getString("tenant_user_full_name"),
                        rs.getString("tenant_user_document"),
                        rs.getString("department_name"),
                        rs.getString("role_name"),
                        rs.getDouble("total_worked_hours"),
                        rs.getInt("total_tardiness_minutes"),
                        rs.getInt("total_absences"),
                        rs.getDouble("total_overtime_hours")),
                month, year);
    }
}
