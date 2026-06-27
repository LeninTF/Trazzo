package trazzo.back.reports.infrastructure.adapters.in.web.dto;

import java.util.UUID;

public record MonthlyClosureDetailResponse(
        UUID id, UUID monthClosureId,
        String tenantUserFullName, String tenantUserDocument,
        String departmentName, String roleName,
        Double totalWorkedHours, Double totalTardinessMinutes,
        int totalAbsences, Double totalOvertimeHours) {
}
