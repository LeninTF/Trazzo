package trazzo.back.reports.application.ports.out;

import java.util.List;

public interface EmployeeAttendanceSummaryPort {
    List<EmployeeMonthlySummary> getMonthlySummaries(int month, int year);

    record EmployeeMonthlySummary(
            String tenantUserId,
            String tenantUserFullName,
            String tenantUserDocument,
            String departmentName,
            String roleName,
            Double totalWorkedHours,
            Double totalTardinessMinutes,
            int totalAbsences,
            Double totalOvertimeHours) {
    }
}
