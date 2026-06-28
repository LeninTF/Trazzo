package trazzo.back.reports.infrastructure.adapters.out.attendance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
    void shouldQueryDatabaseAndReturnEmpty() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt(), anyInt()))
                .thenReturn(List.of());

        List<EmployeeMonthlySummary> summaries = adapter.getMonthlySummaries(6, 2025);

        assertTrue(summaries.isEmpty());
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), eq(6), eq(2025));
    }

    @Test
    void shouldUseRowMapperFromAdapter() throws SQLException {
        when(resultSet.getInt("id")).thenReturn(42);
        when(resultSet.getString("tenant_user_full_name")).thenReturn("Juan Perez");
        when(resultSet.getString("tenant_user_document")).thenReturn("12345678");
        when(resultSet.getString("department_name")).thenReturn("TI");
        when(resultSet.getString("role_name")).thenReturn("Developer");
        when(resultSet.getDouble("total_worked_hours")).thenReturn(160.5);
        when(resultSet.getInt("total_tardiness_minutes")).thenReturn(10);
        when(resultSet.getInt("total_absences")).thenReturn(1);
        when(resultSet.getDouble("total_overtime_hours")).thenReturn(5.0);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt(), anyInt()))
                .thenAnswer(invocation -> {
                    RowMapper<EmployeeMonthlySummary> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(resultSet, 1));
                });

        List<EmployeeMonthlySummary> summaries = adapter.getMonthlySummaries(6, 2025);

        assertEquals(1, summaries.size());
        EmployeeMonthlySummary s = summaries.getFirst();
        assertEquals(42, s.tenantUserId());
        assertEquals("Juan Perez", s.tenantUserFullName());
        assertEquals("12345678", s.tenantUserDocument());
        assertEquals("TI", s.departmentName());
        assertEquals("Developer", s.roleName());
        assertEquals(160.5, s.totalWorkedHours());
        assertEquals(Integer.valueOf(10), s.totalTardinessMinutes());
        assertEquals(1, s.totalAbsences());
        assertEquals(5.0, s.totalOvertimeHours());
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
    }
}
