package trazzo.back.saasglobal.infrastructure.adapters.out.provisioning;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

/**
 * On startup, applies any pending {@code db/tenant/migration/*.sql} scripts to every
 * activated tenant's schema, using the app's single physical database (schema-based
 * multi-tenancy) rather than a separate connection per tenant.
 */
@Slf4j
@Component
public class TenantSchemaMigrator implements ApplicationRunner {

    private static final String MIGRATION_PATH = "db/tenant/migration/";
    private static final Pattern VALID_SCHEMA = Pattern.compile("^[a-z0-9_]+$");

    private final JdbcTemplate jdbc;
    private final DataSource rawDataSource;
    private final ResourcePatternResolver resourceResolver;

    public TenantSchemaMigrator(
            JdbcTemplate jdbc,
            @Qualifier("rawDataSource") DataSource rawDataSource,
            ResourcePatternResolver resourceResolver
    ) {
        this.jdbc = jdbc;
        this.rawDataSource = rawDataSource;
        this.resourceResolver = resourceResolver;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> tenants = jdbc.queryForList("""
                SELECT t.id, ts.schema_name
                FROM tenants t
                JOIN tenant_settings ts ON ts.tenant_id = t.id
                WHERE t.activated_at IS NOT NULL AND t.deleted_at IS NULL
                """);

        for (Map<String, Object> row : tenants) {
            String tenantId = row.get("id") != null ? row.get("id").toString() : "unknown";
            String schemaName = (String) row.get("schema_name");
            try {
                migrateTenant(tenantId, schemaName);
            } catch (Exception e) {
                log.error("Failed to migrate tenant {} (schema {}): {}", tenantId, schemaName, e.getMessage());
            }
        }
    }

    private void migrateTenant(String tenantId, String schemaName) {
        if (schemaName == null || !VALID_SCHEMA.matcher(schemaName).matches()) {
            throw new TenantProvisioningException("Invalid schema name for tenant " + tenantId + ": " + schemaName, null);
        }
        try (Connection conn = rawDataSource.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET search_path TO \"" + schemaName + "\"");
            }
            Resource[] resources = resourceResolver.getResources("classpath:" + MIGRATION_PATH + "*.sql");
            List.of(resources).stream()
                    .sorted(Comparator.comparing(Resource::getFilename))
                    .forEach(script -> {
                        try {
                            ScriptUtils.executeSqlScript(conn, script);
                            log.info("Executed {} on tenant {} (schema {})", script.getFilename(), tenantId, schemaName);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to execute " + script.getFilename(), e);
                        }
                    });
            log.info("Migrated tenant {} (schema {})", tenantId, schemaName);
        } catch (SQLException | IOException e) {
            throw new TenantProvisioningException("Failed to migrate tenant schema: " + schemaName, e);
        }
    }
}
