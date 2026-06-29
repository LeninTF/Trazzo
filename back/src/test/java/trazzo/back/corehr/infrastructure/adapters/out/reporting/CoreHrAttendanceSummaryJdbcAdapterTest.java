package trazzo.back.corehr.infrastructure.adapters.out.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class CoreHrAttendanceSummaryJdbcAdapterTest {

    private JdbcTemplate jdbcTemplate;
    private CoreHrAttendanceSummaryJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        adapter = new CoreHrAttendanceSummaryJdbcAdapter(jdbcTemplate);
    }

    @Test
    void getMonthlySummariesReturnsEmptyWhenNoRowsFound() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyInt(), anyInt())).thenReturn(List.of());

        var summaries = adapter.getMonthlySummaries(6, 2026);

        assertTrue(summaries.isEmpty());
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), eq(6), eq(2026));
    }

    @Test
    void getMonthlySummariesMapsSqlRow() throws SQLException {
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.getInt("id")).thenReturn(7);
        when(resultSet.getString("tenant_user_full_name")).thenReturn("Ana Torres");
        when(resultSet.getString("tenant_user_document")).thenReturn("12345678");
        when(resultSet.getString("department_name")).thenReturn("Operaciones");
        when(resultSet.getString("role_name")).thenReturn("Supervisor");
        when(resultSet.getDouble("total_worked_hours")).thenReturn(168.5);
        when(resultSet.getInt("total_tardiness_minutes")).thenReturn(12);
        when(resultSet.getInt("total_absences")).thenReturn(1);
        when(resultSet.getDouble("total_overtime_hours")).thenReturn(0.0);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(6), eq(2026)))
                .thenAnswer(invocation -> {
                    RowMapper<?> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(resultSet, 0));
                });

        var summaries = adapter.getMonthlySummaries(6, 2026);

        assertEquals(1, summaries.size());
        var summary = summaries.getFirst();
        assertEquals(7, summary.tenantUserId());
        assertEquals("Ana Torres", summary.tenantUserFullName());
        assertEquals("12345678", summary.tenantUserDocument());
        assertEquals("Operaciones", summary.departmentName());
        assertEquals("Supervisor", summary.roleName());
        assertEquals(168.5, summary.totalWorkedHours());
        assertEquals(12, summary.totalTardinessMinutes());
        assertEquals(1, summary.totalAbsences());
        assertEquals(0.0, summary.totalOvertimeHours());
    }
}
