package trazzo.back.reports.application.ports.out;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import trazzo.back.reports.application.ports.out.EmployeeAttendanceSummaryPort.EmployeeMonthlySummary;

import java.util.List;

class EmployeeAttendanceSummaryPortTest {

    @Test
    void shouldBeInterface() {
        assertTrue(EmployeeAttendanceSummaryPort.class.isInterface());
    }

    @Test
    void shouldDefineGetMonthlySummariesMethod() throws NoSuchMethodException {
        EmployeeAttendanceSummaryPort.class.getDeclaredMethod("getMonthlySummaries", int.class, int.class);
    }

    @Test
    void shouldReturnListFromGetMonthlySummaries() throws NoSuchMethodException {
        var method = EmployeeAttendanceSummaryPort.class.getDeclaredMethod("getMonthlySummaries", int.class, int.class);
        assertEquals(List.class, method.getReturnType());
    }

    @Test
    void shouldDefineEmployeeMonthlySummaryRecord() {
        assertNotNull(EmployeeMonthlySummary.class);
        assertTrue(EmployeeMonthlySummary.class.isRecord());
    }

    @Test
    void shouldCreateEmployeeMonthlySummarySuccessfully() {
        EmployeeMonthlySummary summary = new EmployeeMonthlySummary(
                "user-1", "Juan Perez", "12345678",
                "TI", "Developer", 160.0, 10.0, 1, 5.0);

        assertEquals("user-1", summary.tenantUserId());
        assertEquals("Juan Perez", summary.tenantUserFullName());
        assertEquals("12345678", summary.tenantUserDocument());
        assertEquals("TI", summary.departmentName());
        assertEquals("Developer", summary.roleName());
        assertEquals(160.0, summary.totalWorkedHours());
        assertEquals(10.0, summary.totalTardinessMinutes());
        assertEquals(1, summary.totalAbsences());
        assertEquals(5.0, summary.totalOvertimeHours());
    }
}
