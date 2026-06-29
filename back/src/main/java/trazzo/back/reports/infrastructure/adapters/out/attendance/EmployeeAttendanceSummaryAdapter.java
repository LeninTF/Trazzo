package trazzo.back.reports.infrastructure.adapters.out.attendance;

import trazzo.back.corehr.application.port.in.CoreHrAttendanceSummaryPort;
import trazzo.back.reports.application.ports.out.EmployeeAttendanceSummaryPort;

import java.util.List;

public class EmployeeAttendanceSummaryAdapter implements EmployeeAttendanceSummaryPort {

    private final CoreHrAttendanceSummaryPort coreHrAttendanceSummaryPort;

    public EmployeeAttendanceSummaryAdapter(CoreHrAttendanceSummaryPort coreHrAttendanceSummaryPort) {
        this.coreHrAttendanceSummaryPort = coreHrAttendanceSummaryPort;
    }

    @Override
    public List<EmployeeMonthlySummary> getMonthlySummaries(int month, int year) {
        return coreHrAttendanceSummaryPort.getMonthlySummaries(month, year).stream()
                .map(summary -> new EmployeeMonthlySummary(
                        summary.tenantUserId(),
                        summary.tenantUserFullName(),
                        summary.tenantUserDocument(),
                        summary.departmentName(),
                        summary.roleName(),
                        summary.totalWorkedHours(),
                        summary.totalTardinessMinutes(),
                        summary.totalAbsences(),
                        summary.totalOvertimeHours()))
                .toList();
    }
}
