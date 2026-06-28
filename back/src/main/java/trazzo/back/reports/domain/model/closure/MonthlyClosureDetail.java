package trazzo.back.reports.domain.model.closure;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.Getter;

@Getter
public class MonthlyClosureDetail {

    private final UUID id;
    private final UUID monthClosureId;
    private final Integer tenantUserId;
    private final String tenantUserFullName;
    private final String tenantUserDocument;
    private final String departmentName;
    private final String roleName;
    private final Double totalWorkedHours;
    private final Integer totalTardinessMinutes;
    private final int totalAbsences;
    private final Double totalOvertimeHours;
    private final LocalDateTime createdAt;

    public MonthlyClosureDetail(
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

        this.id = Objects.requireNonNull(id, "Id cannot be null");
        this.monthClosureId = Objects.requireNonNull(monthClosureId, "Month closure id cannot be null");
        this.tenantUserId = Objects.requireNonNull(tenantUserId, "Tenant user id cannot be null");
        this.tenantUserFullName = Objects.requireNonNull(tenantUserFullName, "User full name cannot be null");
        this.tenantUserDocument = Objects.requireNonNull(tenantUserDocument, "User document cannot be null");
        this.departmentName = departmentName;
        this.roleName = roleName;
        this.createdAt = Objects.requireNonNull(createdAt, "Created at cannot be null");

        if (totalWorkedHours == null || totalWorkedHours < 0) {
            throw new IllegalArgumentException("Total worked hours cannot be negative");
        }

        if (totalTardinessMinutes == null || totalTardinessMinutes < 0) {
            throw new IllegalArgumentException("Total tardiness minutes cannot be negative");
        }

        if (totalOvertimeHours == null || totalOvertimeHours < 0) {
            throw new IllegalArgumentException("Total overtime hours cannot be negative");
        }

        if (totalAbsences < 0) {
            throw new IllegalArgumentException("Total absences cannot be negative");
        }

        this.totalWorkedHours = totalWorkedHours;
        this.totalTardinessMinutes = totalTardinessMinutes;
        this.totalAbsences = totalAbsences;
        this.totalOvertimeHours = totalOvertimeHours;
    }
}