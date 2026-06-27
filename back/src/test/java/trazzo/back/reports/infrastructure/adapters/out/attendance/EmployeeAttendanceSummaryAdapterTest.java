package trazzo.back.reports.infrastructure.adapters.out.attendance;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import trazzo.back.reports.application.ports.out.EmployeeAttendanceSummaryPort.EmployeeMonthlySummary;

import java.util.List;

class EmployeeAttendanceSummaryAdapterTest {

    private final EmployeeAttendanceSummaryAdapter adapter = new EmployeeAttendanceSummaryAdapter();

    @Test
    void shouldReturnEmptyList() {
        List<EmployeeMonthlySummary> summaries = adapter.getMonthlySummaries(6, 2025);
        assertNotNull(summaries);
        assertTrue(summaries.isEmpty());
    }

    @Test
    void shouldReturnEmptyListForAnyPeriod() {
        List<EmployeeMonthlySummary> summaries = adapter.getMonthlySummaries(1, 2024);
        assertTrue(summaries.isEmpty());
    }
}
