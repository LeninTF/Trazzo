package trazzo.back.reports.application.dto.result;

import java.time.LocalDateTime;
import java.util.UUID;

public record MonthlyClosureDetailResult(
        UUID id,
        UUID monthClosureId,
        Integer tenantUserId,
        String tenantUserFullName,
        String tenantUserDocument,
        String departmentName,
        String roleName,
        Double totalWorkedHours,
        Integer totalTardinessMinutes,
        int totalAbsences,
        Double totalOvertimeHours,
        LocalDateTime createdAt) {

}