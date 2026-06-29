package trazzo.back.reports.infrastructure.adapters.out.attendance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.corehr.application.port.in.CoreHrAttendanceSummaryPort;
import trazzo.back.reports.application.ports.out.EmployeeAttendanceSummaryPort.EmployeeMonthlySummary;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class EmployeeAttendanceSummaryAdapterTest {

    @Mock
    private CoreHrAttendanceSummaryPort coreHrAttendanceSummaryPort;

    private EmployeeAttendanceSummaryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new EmployeeAttendanceSummaryAdapter(coreHrAttendanceSummaryPort);
    }

    @Test
    void shouldDelegateToCoreHrAndReturnEmpty() {
        when(coreHrAttendanceSummaryPort.getMonthlySummaries(anyInt(), anyInt())).thenReturn(List.of());

        List<EmployeeMonthlySummary> summaries = adapter.getMonthlySummaries(6, 2025);

        assertTrue(summaries.isEmpty());
        verify(coreHrAttendanceSummaryPort).getMonthlySummaries(6, 2025);
    }

    @Test
    void shouldMapCoreHrSummaryToReportsSummary() {
        var coreHrSummary = new CoreHrAttendanceSummaryPort.EmployeeMonthlySummary(
                42L, "Juan Perez", "12345678", "TI", "Developer", 160.5, 10, 1, 5.0);
        when(coreHrAttendanceSummaryPort.getMonthlySummaries(6, 2025)).thenReturn(List.of(coreHrSummary));

        List<EmployeeMonthlySummary> summaries = adapter.getMonthlySummaries(6, 2025);

        assertEquals(1, summaries.size());
        EmployeeMonthlySummary s = summaries.getFirst();
        assertEquals(42L, s.tenantUserId());
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
        var summary = new CoreHrAttendanceSummaryPort.EmployeeMonthlySummary(
                1L, "Ana", "87654321", "HR", "Manager", 80.0, 5, 0, 2.0);
        when(coreHrAttendanceSummaryPort.getMonthlySummaries(6, 2025)).thenReturn(List.of(summary));

        List<EmployeeMonthlySummary> summaries = adapter.getMonthlySummaries(6, 2025);

        assertEquals(1, summaries.size());
        assertEquals("Ana", summaries.getFirst().tenantUserFullName());
    }
}
