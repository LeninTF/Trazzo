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
    private String dbName;
    private String dbHost;
    private String dbPort;
    private String dbUser;
    private String dbPassword;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    Clock clock = Clock.systemDefaultZone();

    @SuppressWarnings("java:S107")
    private TenantSettings(
            String tenantId,
            String dbName,
            String dbHost,
            String dbPort,
            String dbUser,
            String dbPassword,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.tenantId = tenantId; // nullable until associated with a persisted Tenant
        this.dbName = requireText(dbName, "dbName");
        this.dbHost = requireText(dbHost, "dbHost");
        this.dbPort = requireText(dbPort, "dbPort");
        this.dbUser = requireText(dbUser, "dbUser");
        this.dbPassword = requireText(dbPassword, "dbPassword");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @SuppressWarnings("java:S107")
    public static TenantSettings of(
            String tenantId,
            String dbHost,
            String dbPort,
            String dbName,
            String dbUser,
            String dbPassword
    ) {
        LocalDateTime now = LocalDateTime.now(Clock.systemDefaultZone());
        return new TenantSettings(tenantId, dbName, dbHost, dbPort, dbUser, dbPassword, now, now);
    }

    @SuppressWarnings("java:S107")
    public static TenantSettings restore(
            String tenantId,
            String dbName,
            String dbHost,
            String dbPort,
            String dbUser,
            String dbPassword,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new TenantSettings(tenantId, dbName, dbHost, dbPort, dbUser, dbPassword, createdAt, updatedAt);
    }

    public void rotatePassword(String newPassword) {
        this.dbPassword = requireText(newPassword, "newPassword");
        this.updatedAt = LocalDateTime.now(clock);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
