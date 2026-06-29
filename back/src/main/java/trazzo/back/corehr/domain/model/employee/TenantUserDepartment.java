package trazzo.back.corehr.domain.model.employee;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.corehr.domain.exception.CoreHrValidationException;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TenantUserDepartment {

    private Long id;
    private Long tenantUserId;
    private Long departmentId;
    private boolean isPrimary;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    transient Clock clock = Clock.systemDefaultZone();

    private TenantUserDepartment(
            Long id,
            Long tenantUserId,
            Long departmentId,
            boolean isPrimary,
            LocalDate startDate,
            LocalDate endDate,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.tenantUserId = requireTenantUserId(tenantUserId);
        this.departmentId = requireDepartmentId(departmentId);
        this.isPrimary = isPrimary;
        this.startDate = requireDate(startDate, "startDate");
        this.endDate = endDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TenantUserDepartment create(Long tenantUserId, Long departmentId, boolean isPrimary, LocalDate startDate, LocalDate endDate) {
        LocalDateTime now = LocalDateTime.now();
        return new TenantUserDepartment(null, tenantUserId, departmentId, isPrimary, startDate, endDate, now, now);
    }

    public static TenantUserDepartment restore(
            Long id,
            Long tenantUserId,
            Long departmentId,
            boolean isPrimary,
            LocalDate startDate,
            LocalDate endDate,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new TenantUserDepartment(id, tenantUserId, departmentId, isPrimary, startDate, endDate, createdAt, updatedAt);
    }

    public void markAsPrimary() {
        this.isPrimary = true;
        touch();
    }

    public void unmarkAsPrimary() {
        this.isPrimary = false;
        touch();
    }

    public void endAssignment(LocalDate endDate) {
        this.endDate = requireDate(endDate, "endDate");
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now(clock);
    }

    private static Long requireTenantUserId(Long tenantUserId) {
        if (tenantUserId == null) {
            throw new CoreHrValidationException("tenantUserId is required");
        }
        return tenantUserId;
    }

    private static Long requireDepartmentId(Long departmentId) {
        if (departmentId == null) {
            throw new CoreHrValidationException("departmentId is required");
        }
        return departmentId;
    }

    private static LocalDate requireDate(LocalDate value, String fieldName) {
        if (value == null) {
            throw new CoreHrValidationException(fieldName + " is required");
        }
        return value;
    }
}
