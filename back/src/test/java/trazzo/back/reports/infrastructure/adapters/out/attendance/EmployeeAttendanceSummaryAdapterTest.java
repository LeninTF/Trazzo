package trazzo.back.reports.infrastructure.adapters.out.attendance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import trazzo.back.reports.application.ports.out.EmployeeAttendanceSummaryPort.EmployeeMonthlySummary;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class EmployeeAttendanceSummaryAdapterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ResultSet resultSet;

    private EmployeeAttendanceSummaryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new EmployeeAttendanceSummaryAdapter(jdbcTemplate);
    }

    @Test
    void shouldQueryDatabase() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt(), anyInt()))
                .thenReturn(List.of());

        List<EmployeeMonthlySummary> summaries = adapter.getMonthlySummaries(6, 2025);
        assertNotNull(summaries);
        assertTrue(summaries.isEmpty());
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), eq(6), eq(2025));
    }

    @Test
    void shouldMapRowCorrectly() throws SQLException {
        when(resultSet.getInt("id")).thenReturn(42);
        when(resultSet.getString("tenant_user_full_name")).thenReturn("Juan Perez");
        when(resultSet.getString("tenant_user_document")).thenReturn("12345678");
        when(resultSet.getString("department_name")).thenReturn("TI");
        when(resultSet.getString("role_name")).thenReturn("Developer");
        when(resultSet.getDouble("total_worked_hours")).thenReturn(160.5);
        when(resultSet.getInt("total_tardiness_minutes")).thenReturn(10);
        when(resultSet.getInt("total_absences")).thenReturn(1);
        when(resultSet.getDouble("total_overtime_hours")).thenReturn(5.0);

        RowMapper<EmployeeMonthlySummary> mapper = (rs, rowNum) -> new EmployeeMonthlySummary(
                rs.getInt("id"),
                rs.getString("tenant_user_full_name"),
                rs.getString("tenant_user_document"),
                rs.getString("department_name"),
                rs.getString("role_name"),
                rs.getDouble("total_worked_hours"),
                rs.getInt("total_tardiness_minutes"),
                rs.getInt("total_absences"),
                rs.getDouble("total_overtime_hours"));

        EmployeeMonthlySummary summary = mapper.mapRow(resultSet, 1);

        assertEquals(42, summary.tenantUserId());
        assertEquals("Juan Perez", summary.tenantUserFullName());
        assertEquals("12345678", summary.tenantUserDocument());
        assertEquals("TI", summary.departmentName());
        assertEquals("Developer", summary.roleName());
        assertEquals(160.5, summary.totalWorkedHours());
        assertEquals(Integer.valueOf(10), summary.totalTardinessMinutes());
        assertEquals(1, summary.totalAbsences());
        assertEquals(5.0, summary.totalOvertimeHours());
    }

    @Test
    void shouldReturnSummariesWithData() {
        EmployeeMonthlySummary summary = new EmployeeMonthlySummary(
                1, "Ana", "87654321", "HR", "Manager", 80.0, 5, 0, 2.0);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(6), eq(2025)))
                .thenReturn(List.of(summary));

        List<EmployeeMonthlySummary> summaries = adapter.getMonthlySummaries(6, 2025);

        assertEquals(1, summaries.size());
        assertEquals("Ana", summaries.getFirst().tenantUserFullName());
        assertEquals("HR", summaries.getFirst().departmentName());
    }
}
