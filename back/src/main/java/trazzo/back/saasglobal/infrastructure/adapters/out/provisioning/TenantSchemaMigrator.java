package trazzo.back.saasglobal.infrastructure.adapters.out.provisioning;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * On startup, applies any pending {@code db/tenant/migration/V*.sql} script to every
 * activated tenant's schema through {@link TenantFlywayMigrator}, using the app's single
 * physical database (schema-based multi-tenancy).
 *
 * A tenant whose migration fails is handled according to
 * {@code trazzo.tenant-migrations.fail-on-error}: {@code false} (default) logs the error with
 * its stack trace and carries on with the remaining tenants, {@code true} (local and tests)
 * aborts the startup. Either way {@link TenantMigrationsHealthIndicator} reports the failures.
 *
 * Ordered after TenantDataSeeder (@Order(1)): without an explicit order, Spring's
 * ApplicationRunner/CommandLineRunner execution order is not guaranteed, so this could
 * run before the local-dev demo tenant is created — finding zero activated tenants and
 * silently doing nothing (no log line, no error) instead of seeding its schema.
 */
@Slf4j
@Component
@Order(2)
public class TenantSchemaMigrator implements ApplicationRunner {

    private final JdbcTemplate jdbc;
    private final TenantFlywayMigrator flywayMigrator;
    private final boolean failOnError;
    private final AtomicInteger failedTenants = new AtomicInteger();

    public TenantSchemaMigrator(
            JdbcTemplate jdbc,
            TenantFlywayMigrator flywayMigrator,
            @Value("${trazzo.tenant-migrations.fail-on-error:false}") boolean failOnError
    ) {
        this.jdbc = jdbc;
        this.flywayMigrator = flywayMigrator;
        this.failOnError = failOnError;
    }

    @Override
    public void run(ApplicationArguments args) {
        failedTenants.set(0);
        List<Map<String, Object>> tenants = jdbc.queryForList("""
                SELECT t.id, ts.schema_name
                FROM tenants t
                JOIN tenant_settings ts ON ts.tenant_id = t.id
                WHERE t.activated_at IS NOT NULL AND t.deleted_at IS NULL
                """);

        if (tenants.isEmpty()) {
            log.info("No activated tenants found; nothing to migrate.");
            return;
        }

        for (Map<String, Object> row : tenants) {
            String tenantId = row.get("id") != null ? row.get("id").toString() : "unknown";
            String schemaName = (String) row.get("schema_name");
            try {
                int applied = flywayMigrator.migrate(schemaName);
                log.info("Migrated tenant {} (schema {}): {} migration(s) applied", tenantId, schemaName, applied);
            } catch (Exception e) {
                failedTenants.incrementAndGet();
                if (failOnError) {
                    throw new TenantProvisioningException(
                            "Failed to migrate tenant " + tenantId + " (schema " + schemaName + ")", e);
                }
                log.error("Failed to migrate tenant {} (schema {})", tenantId, schemaName, e);
            }
        }
    }

    /** Tenants whose migration failed during the last run. */
    public int failedTenants() {
        return failedTenants.get();
    }
}
