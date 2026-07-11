package trazzo.back.saasglobal.infrastructure.config;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.saasglobal.application.port.out.TenantRepositoryPort;
import trazzo.back.saasglobal.application.port.out.TenantSchemaProvisioningPort;
import trazzo.back.saasglobal.domain.model.multitenancy.Tenant;
import trazzo.back.saasglobal.domain.model.multitenancy.TenantSettings;

/**
 * Local-dev only: provisions a demo tenant (its own PostgreSQL schema, running
 * db/tenant/schema.sql) so organization/corehr/incidents can be exercised via
 * docker compose up without manually calling POST /tenants/trial first.
 */
@Component
@Profile("local")
@ConditionalOnProperty(name = "trazzo.seed.tenant.enabled", havingValue = "true", matchIfMissing = false)
@Order(1)
@Slf4j
public class TenantDataSeeder implements CommandLineRunner {

    private static final String DEMO_PLAN_NAME = "Plan Demo";

    private final TenantRepositoryPort tenantRepository;
    private final TenantSchemaProvisioningPort schemaProvisioning;
    private final JdbcTemplate jdbc;
    private final String subDomain;

    public TenantDataSeeder(
            TenantRepositoryPort tenantRepository,
            TenantSchemaProvisioningPort schemaProvisioning,
            JdbcTemplate jdbc,
            @Value("${trazzo.seed.tenant.sub-domain}") String subDomain
    ) {
        this.tenantRepository = tenantRepository;
        this.schemaProvisioning = schemaProvisioning;
        this.jdbc = jdbc;
        this.subDomain = requireNonBlank(subDomain, "trazzo.seed.tenant.sub-domain");
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (tenantRepository.findBySubDomain(subDomain).isPresent()) {
            log.info("Demo tenant already exists, skipping seed");
            return;
        }

        log.info("Provisioning demo tenant '{}'...", subDomain);

        Integer planId = ensureDemoPlanExists();
        TenantSettings settings = TenantSettings.of(null, TenantSettings.deriveSchemaName(subDomain));
        schemaProvisioning.provisionExisting(settings);

        Tenant tenant = Tenant.createTrial(subDomain, planId, null, settings, null);
        tenant.activate();
        tenantRepository.save(tenant);

        log.info("Demo tenant '{}' provisioned successfully (schema: {})", subDomain, settings.getSchemaName());
    }

    private Integer ensureDemoPlanExists() {
        List<Integer> existing = jdbc.queryForList(
                "SELECT id FROM plans WHERE name = ?", Integer.class, DEMO_PLAN_NAME);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        return jdbc.queryForObject("""
                INSERT INTO plans (name, price, currency, billing_period, is_active)
                VALUES (?, 0, 'SOLES', 'MONTHLY', TRUE)
                RETURNING id
                """, Integer.class, DEMO_PLAN_NAME);
    }

    private static String requireNonBlank(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Property '" + propertyName + "' must be set when seeding is enabled"
            );
        }
        return value.trim();
    }
}
