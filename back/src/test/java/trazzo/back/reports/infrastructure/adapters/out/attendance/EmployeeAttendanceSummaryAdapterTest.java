package trazzo.back.reports.infrastructure.adapters.out.attendance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import trazzo.back.reports.application.ports.out.EmployeeAttendanceSummaryPort.EmployeeMonthlySummary;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class EmployeeAttendanceSummaryAdapterTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private EmployeeAttendanceSummaryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new EmployeeAttendanceSummaryAdapter(jdbcTemplate);
    }

    @Test
    void shouldQueryDatabase() {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), anyInt(), anyInt()))
                .thenReturn(List.of());

        List<EmployeeMonthlySummary> summaries = adapter.getMonthlySummaries(6, 2025);
        assertNotNull(summaries);
        assertTrue(summaries.isEmpty());
        verify(jdbcTemplate).query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), eq(6), eq(2025));
    }
}
