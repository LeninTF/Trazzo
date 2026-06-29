package trazzo.back.reports.infrastructure.adapters.in.web.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MonthlyClosureDetailResponse(
        UUID id, UUID monthClosureId, Long tenantUserId,
        String tenantUserFullName, String tenantUserDocument,
        String departmentName, String roleName,
        Double totalWorkedHours, Integer totalTardinessMinutes,
        int totalAbsences, Double totalOvertimeHours,
        LocalDateTime createdAt) {
}
