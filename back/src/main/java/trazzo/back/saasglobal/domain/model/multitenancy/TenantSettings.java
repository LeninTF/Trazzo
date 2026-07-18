package trazzo.back.saasglobal.domain.model.multitenancy;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.saasglobal.domain.exception.TenantValidationException;

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
     * already enforced at the application level, so the derived schema name is unique too —
     * this only holds if the normalized name is never truncated, so a too-long subDomain is
     * rejected instead of silently truncated (truncation could map two distinct, unique
     * subdomains to the same schema name).
     */
    public static String deriveSchemaName(String subDomain) {
        String safe = subDomain.toLowerCase().replaceAll("[^a-z0-9]", "_");
        if (safe.length() > MAX_SCHEMA_SUFFIX_LENGTH) {
            throw new TenantValidationException(
                    "subDomain is too long to derive a unique schema name (max "
                            + MAX_SCHEMA_SUFFIX_LENGTH + " normalized characters): " + subDomain);
        }
        return "tenant_" + safe;
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
