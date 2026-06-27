package trazzo.back.reports.infrastructure.adapters.out.attendance;

import trazzo.back.reports.application.ports.out.EmployeeAttendanceSummaryPort;

import java.util.List;

public class EmployeeAttendanceSummaryAdapter implements EmployeeAttendanceSummaryPort {

    @Override
    public List<EmployeeMonthlySummary> getMonthlySummaries(int month, int year) {
        return List.of();
    }
}
