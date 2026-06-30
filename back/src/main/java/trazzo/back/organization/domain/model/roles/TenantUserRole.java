package trazzo.back.organization.domain.model.roles;

import lombok.Getter;
import trazzo.back.organization.domain.exception.OrgValidationException;

import java.time.LocalDateTime;

@Getter
public class TenantUserRole {

    private final Long id;
    private final Long tenantUserId;
    private final String roleId;
    private final Long departmentId;
    private final LocalDateTime createdAt;

    private TenantUserRole(Long id, Long tenantUserId, String roleId, Long departmentId, LocalDateTime createdAt) {
        this.id = id;
        this.tenantUserId = requireId(tenantUserId, "tenantUserId");
        this.roleId = requireText(roleId, "roleId");
        this.departmentId = departmentId;
        this.createdAt = createdAt;
    }

    public static TenantUserRole create(Long tenantUserId, String roleId, Long departmentId) {
        return new TenantUserRole(null, tenantUserId, roleId, departmentId, LocalDateTime.now());
    }

    public static TenantUserRole restore(Long id, Long tenantUserId, String roleId,
                                          Long departmentId, LocalDateTime createdAt) {
        return new TenantUserRole(id, tenantUserId, roleId, departmentId, createdAt);
    }

    private static Long requireId(Long value, String fieldName) {
        if (value == null) {
            throw new OrgValidationException(fieldName + " is required");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new OrgValidationException(fieldName + " is required");
        }
        return value.trim();
    }
}
