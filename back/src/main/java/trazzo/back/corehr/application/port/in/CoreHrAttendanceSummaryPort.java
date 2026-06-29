package trazzo.back.corehr.application.port.in;

import java.util.List;

public interface CoreHrAttendanceSummaryPort {

    List<EmployeeMonthlySummary> getMonthlySummaries(int month, int year);

    record EmployeeMonthlySummary(
            Long tenantUserId,
            String tenantUserFullName,
            String tenantUserDocument,
            String departmentName,
            String roleName,
            Double totalWorkedHours,
            Integer totalTardinessMinutes,
            int totalAbsences,
            Double totalOvertimeHours) {
    }
}
