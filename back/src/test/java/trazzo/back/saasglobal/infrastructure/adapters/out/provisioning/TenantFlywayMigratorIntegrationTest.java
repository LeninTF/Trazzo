package trazzo.back.saasglobal.infrastructure.adapters.out.provisioning;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Runs the real tenant scripts against PostgreSQL 15; H2 cannot execute them. Requires Docker. */
@Testcontainers
class TenantFlywayMigratorIntegrationTest {

    /** Baseline row (version 0) plus V2, V3, V3.1 and V4. Raise when a new tenant migration is added. */
    private static final int HISTORY_ROWS = 5;
    private static final int MIGRATIONS = 4;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    private DataSource dataSource;
    private JdbcTemplate jdbc;
    private TenantFlywayMigrator migrator;

    @BeforeEach
    void setUp() throws SQLException {
        dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        migrator = new TenantFlywayMigrator(dataSource);
        jdbc.execute("DROP SCHEMA IF EXISTS tenant_a CASCADE");
        jdbc.execute("DROP SCHEMA IF EXISTS tenant_b CASCADE");
        createSchemaWithBaseline("tenant_a");
        createSchemaWithBaseline("tenant_b");
    }

    /** Same steps as TenantSchemaProvisioningAdapter before it hands over to Flyway. */
    private void createSchemaWithBaseline(String schema) throws SQLException {
        jdbc.execute("CREATE SCHEMA \"" + schema + "\"");
        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("SET search_path TO \"" + schema + "\", public");
            ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/tenant/V1__tenant_db.sql"));
        }
    }

    private int historyRows(String schema) {
        return jdbc.queryForObject("SELECT count(*) FROM \"" + schema + "\".flyway_tenant_history", Integer.class);
    }

    private int failedHistoryRows(String schema) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM \"" + schema + "\".flyway_tenant_history WHERE NOT success", Integer.class);
    }

    @Test
    void migratesEverySchemaAndTheSecondRunAppliesNothing() {
        assertThat(migrator.migrate("tenant_a")).isEqualTo(MIGRATIONS);
        assertThat(migrator.migrate("tenant_b")).isEqualTo(MIGRATIONS);

        assertThat(migrator.migrate("tenant_a")).isZero();
        assertThat(migrator.migrate("tenant_b")).isZero();

        assertThat(historyRows("tenant_a")).isEqualTo(HISTORY_ROWS);
        assertThat(historyRows("tenant_b")).isEqualTo(HISTORY_ROWS);
        assertThat(failedHistoryRows("tenant_a")).isZero();
        assertThat(failedHistoryRows("tenant_b")).isZero();
    }

    @Test
    void historyIsKeptPerSchemaAndNeverInPublic() {
        migrator.migrate("tenant_a");

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'flyway_tenant_history'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_name = 'flyway_tenant_history'"
                        + " AND table_schema = 'tenant_a'", Integer.class)).isEqualTo(1);
    }

    @Test
    void v2GuardsLookOnlyAtTheSchemaBeingMigrated() {
        // tenant_a plays an old schema that predates the constraints; tenant_b is already healthy
        // and is migrated first, so its constraint must not hide the missing one in tenant_a.
        jdbc.execute("ALTER TABLE tenant_a.monthly_closures DROP CONSTRAINT uq_monthly_closures_period");
        jdbc.execute("ALTER TABLE tenant_a.monthly_closures ALTER COLUMN created_by_user_id DROP NOT NULL");
        jdbc.execute("INSERT INTO tenant_a.monthly_closures (month, year, total_employees, created_by_user_id)"
                + " VALUES (1, 2026, 3, NULL)");

        migrator.migrate("tenant_b");
        migrator.migrate("tenant_a");

        assertThat(jdbc.queryForObject("""
                SELECT count(*) FROM pg_constraint c JOIN pg_namespace n ON n.oid = c.connamespace
                WHERE n.nspname = 'tenant_a' AND c.conname = 'uq_monthly_closures_period'
                """, Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT is_nullable FROM information_schema.columns
                WHERE table_schema = 'tenant_a' AND table_name = 'monthly_closures'
                  AND column_name = 'created_by_user_id'
                """, String.class)).isEqualTo("NO");
        assertThat(jdbc.queryForObject(
                "SELECT created_by_user_id::text FROM tenant_a.monthly_closures", String.class))
                .isEqualTo("00000000-0000-0000-0000-000000000000");
    }

    @Test
    void seedMigrationIsAppliedOncePerSchema() {
        migrator.migrate("tenant_a");
        migrator.migrate("tenant_b");

        int permissionsA = jdbc.queryForObject("SELECT count(*) FROM tenant_a.permissions", Integer.class);
        int permissionsB = jdbc.queryForObject("SELECT count(*) FROM tenant_b.permissions", Integer.class);
        assertThat(permissionsA).isPositive().isEqualTo(permissionsB);
    }
}
