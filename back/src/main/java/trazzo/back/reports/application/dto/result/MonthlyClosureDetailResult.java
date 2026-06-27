package trazzo.back.reports.application.dto.result;

import java.time.LocalDateTime;
import java.util.UUID;

public record MonthlyClosureDetailResult(
        UUID id,
        UUID monthClosureId,
        String tenantUserFullName,
        String tenantUserDocument,
        String departmentName,
        String roleName,
        Double totalWorkedHours,
        Double totalTardinessMinutes,
        int totalAbsences,
        Double totalOvertimeHours,
        LocalDateTime createdAt) {

}