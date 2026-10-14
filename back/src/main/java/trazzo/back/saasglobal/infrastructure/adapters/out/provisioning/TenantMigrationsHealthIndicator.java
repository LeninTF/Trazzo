package trazzo.back.saasglobal.infrastructure.adapters.out.provisioning;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health contributor {@code tenantMigrations}: OUT_OF_SERVICE while the last startup run left
 * any tenant schema unmigrated. It carries no details on purpose (no tenant ids or schema
 * names), so nothing about tenants is exposed to anonymous callers.
 */
@Component
public class TenantMigrationsHealthIndicator implements HealthIndicator {

    private final TenantSchemaMigrator migrator;

    public TenantMigrationsHealthIndicator(TenantSchemaMigrator migrator) {
        this.migrator = migrator;
    }

    @Override
    public Health health() {
        return migrator.failedTenants() > 0 ? Health.outOfService().build() : Health.up().build();
    }
}
