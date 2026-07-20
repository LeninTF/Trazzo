package trazzo.back.saasglobal.infrastructure.config;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.saasglobal.application.port.out.TenantRepositoryPort;
import trazzo.back.saasglobal.application.port.out.TenantSchemaProvisioningPort;
import trazzo.back.saasglobal.application.port.out.UserRepositoryPort;
import trazzo.back.saasglobal.domain.model.iam.User;
import trazzo.back.saasglobal.domain.model.multitenancy.Tenant;
import trazzo.back.saasglobal.domain.model.multitenancy.TenantSettings;

/**
 * Local-dev only: provisions a demo tenant (its own PostgreSQL schema, running
 * db/tenant/V1__tenant_db.sql) so organization/corehr/incidents can be exercised via
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
    private final PasswordEncoder passwordEncoder;
    private final UserRepositoryPort userRepository;
    private final String subDomain;

    public TenantDataSeeder(
            TenantRepositoryPort tenantRepository,
            TenantSchemaProvisioningPort schemaProvisioning,
            JdbcTemplate jdbc,
            PasswordEncoder passwordEncoder,
            UserRepositoryPort userRepository,
            @Value("${trazzo.seed.tenant.sub-domain}") String subDomain
    ) {
        this.tenantRepository = tenantRepository;
        this.schemaProvisioning = schemaProvisioning;
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
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
        // Drop any orphaned schema from a previous failed run, then provision fresh.
        // This is safe because TenantDataSeeder is local-dev only.
        schemaProvisioning.recreateSchema(settings.getSchemaName());
        schemaProvisioning.provisionExisting(settings);

        Tenant tenant = Tenant.createTrial(subDomain, planId, null, settings, null);
        tenant.activate();
        tenantRepository.save(tenant);

        log.info("Demo tenant '{}' provisioned successfully (schema: {})", subDomain, settings.getSchemaName());

        createTenantUser(tenant, settings.getSchemaName());
    }

    private void createTenantUser(Tenant tenant, String schemaName) {
        if (userRepository.findByEmail("demo@trazzo.pe").isPresent()) {
            log.info("Tenant user already exists, skipping");
            return;
        }

        log.info("Creating tenant user for '{}'...", subDomain);

        Integer personId = insertPerson();
        String encodedPassword = passwordEncoder.encode("demo123");

        User tenantUser = User.create(
                personId,
                tenant.getId(),
                "demo@trazzo.pe",
                null,
                encodedPassword
        );
        userRepository.save(tenantUser);

        jdbc.execute("SET search_path TO \"" + schemaName + "\", public");
        try {
            jdbc.update("""
                    INSERT INTO tenant_user (master_user_id, state, created_at, updated_at)
                    VALUES (?::uuid, 'ACTIVO', NOW(), NOW())
                    """, tenantUser.getId());

            Long tenantUserId = jdbc.queryForObject("SELECT currval('tenant_user_id_seq')", Long.class);
            String roleId = ensureAdminRoleExists();

            jdbc.update("""
                    INSERT INTO tenant_user_role (tenant_user_id, role_id, created_at)
                    VALUES (?, ?::uuid, NOW())
                    """, tenantUserId, roleId);

            log.info("Tenant user '{}' created with role 'administrador'", "demo@trazzo.pe");
        } finally {
            jdbc.execute("SET search_path TO public");
        }
    }

    @SuppressWarnings("java:S5145")
    private Integer insertPerson() {
        jdbc.update("""
                INSERT INTO persons (document_type, document_value, name, father_surname, mother_surname)
                VALUES ('DNI', '00000001', 'Demo', 'Trazzo', 'Usuario')
                """);
        return jdbc.queryForObject("SELECT LASTVAL()", Integer.class);
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

    /**
     * Ensures the 'administrador' role exists in the tenant schema and returns its ID.
     * TenantSchemaMigrator (@Order(2)) will later run V3__seed_default_roles_permissions.sql
     * which also seeds this role idempotently (WHERE NOT EXISTS).
     */
    private String ensureAdminRoleExists() {
        return jdbc.queryForObject("""
                INSERT INTO role (id, name, description)
                SELECT gen_random_uuid(), 'administrador', 'El Administrador tiene acceso total a la configuración y seguridad del sistema'
                WHERE NOT EXISTS (SELECT 1 FROM role WHERE name = 'administrador')
                RETURNING id
                """, String.class);
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
