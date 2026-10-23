package trazzo.back.shared.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import trazzo.back.saasglobal.infrastructure.adapters.out.provisioning.TenantFlywayMigrator;
import trazzo.back.shared.tenancy.TenantContext;

import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** El resolver contra las migraciones reales de tenant en PostgreSQL 15. Requiere Docker. */
@Testcontainers
class TenantUserIdResolverIntegrationTest {

    private static final String SCHEMA = "tenant_authz";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @BeforeAll
    static void migrateTenantSchema() throws Exception {
        var admin = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        new JdbcTemplate(admin).execute("CREATE SCHEMA " + SCHEMA);
        try (Connection conn = admin.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("SET search_path TO " + SCHEMA + ", public");
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/tenant/V1__tenant_db.sql"));
        }
        new TenantFlywayMigrator(admin).migrate(SCHEMA);
    }

    private final JdbcTemplate jdbc = tenantJdbc();
    private final TenantUserIdResolver resolver = new TenantUserIdResolver(jdbc);

    private static JdbcTemplate tenantJdbc() {
        String url = POSTGRES.getJdbcUrl();
        url += (url.contains("?") ? "&" : "?") + "currentSchema=" + SCHEMA;
        return new JdbcTemplate(new DriverManagerDataSource(url, POSTGRES.getUsername(), POSTGRES.getPassword()));
    }

    private long tenantUserOf(UUID masterUserId, boolean deleted) {
        return jdbc.queryForObject("""
                INSERT INTO tenant_user (master_user_id, deleted_at)
                VALUES (?::uuid, CASE WHEN ? THEN CURRENT_TIMESTAMP END) RETURNING id
                """, Long.class, masterUserId.toString(), deleted);
    }

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void resolvesTheTenantUserOfAMasterUser() {
        UUID master = UUID.randomUUID();
        long id = tenantUserOf(master, false);
        TenantContext.set(SCHEMA);

        assertThat(resolver.resolve(master)).contains(id);
    }

    @Test
    void aDeletedTenantUserIsNotResolved() {
        UUID master = UUID.randomUUID();
        tenantUserOf(master, true);
        TenantContext.set(SCHEMA);

        assertThat(resolver.resolve(master)).isEmpty();
    }

    @Test
    void aMasterUserWithoutTenantUserIsNotResolved() {
        TenantContext.set(SCHEMA);

        assertThat(resolver.resolve(UUID.randomUUID())).isEmpty();
        assertThat(resolver.resolve(null)).isEmpty();
    }

    @Test
    void outsideATenantNothingIsResolved() {
        UUID master = UUID.randomUUID();
        tenantUserOf(master, false);

        assertThat(resolver.resolve(master)).isEmpty();
    }
}
