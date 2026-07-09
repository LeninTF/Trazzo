package trazzo.back.saasglobal.domain.model.multitenancy;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TenantSettings {

    private String tenantId;
    private String schemaName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    Clock clock = Clock.systemDefaultZone();

    private TenantSettings(
            String tenantId,
            String schemaName,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.tenantId = tenantId; // nullable until associated with a persisted Tenant
        this.schemaName = requireText(schemaName, "schemaName");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static TenantSettings of(String tenantId, String schemaName) {
        LocalDateTime now = LocalDateTime.now(Clock.systemDefaultZone());
        return new TenantSettings(tenantId, schemaName, now, now);
    }

    private static final int MAX_SCHEMA_SUFFIX_LENGTH = 55; // "tenant_" (7) + 55 stays under Postgres' 63-byte identifier limit

    /**
     * Derives a PostgreSQL-safe schema name from a subdomain. subDomain uniqueness is
     * already enforced at the application level, so the derived schema name is unique too.
     */
    public static String deriveSchemaName(String subDomain) {
        String safe = subDomain.toLowerCase().replaceAll("[^a-z0-9]", "_");
        String truncated = safe.length() > MAX_SCHEMA_SUFFIX_LENGTH ? safe.substring(0, MAX_SCHEMA_SUFFIX_LENGTH) : safe;
        return "tenant_" + truncated;
    }

    public static TenantSettings restore(
            String tenantId,
            String schemaName,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new TenantSettings(tenantId, schemaName, createdAt, updatedAt);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
