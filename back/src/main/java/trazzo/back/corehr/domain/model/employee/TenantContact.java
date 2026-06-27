package trazzo.back.corehr.domain.model.employee;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.corehr.domain.exception.CoreHrValidationException;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TenantContact {

    private Long id;
    private Long tenantUserId;
    private String type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    transient Clock clock = Clock.systemDefaultZone();

    private TenantContact(
            Long id,
            Long tenantUserId,
            String type,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        this.id = id;
        this.tenantUserId = requireTenantUserId(tenantUserId);
        this.type = requireText(type, "type");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static TenantContact create(Long tenantUserId, String type) {
        LocalDateTime now = LocalDateTime.now();
        return new TenantContact(null, tenantUserId, type, now, now, null);
    }

    public static TenantContact restore(
            Long id,
            Long tenantUserId,
            String type,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime deletedAt
    ) {
        return new TenantContact(id, tenantUserId, type, createdAt, updatedAt, deletedAt);
    }

    public void updateType(String type) {
        this.type = requireText(type, "type");
        touch();
    }

    public void markAsDeleted() {
        this.deletedAt = LocalDateTime.now(clock);
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

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new CoreHrValidationException(fieldName + " is required");
        }
        return value.trim();
    }
}
