package trazzo.back.saasglobal.infrastructure.adapters.out.provisioning;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import trazzo.back.saasglobal.infrastructure.config.ProvisioningProperties;
import trazzo.back.shared.security.EncryptionService;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantSchemaMigrator implements ApplicationRunner {

    private static final String MIGRATION_PATH = "db/tenant/migration/";

    private final JdbcTemplate jdbc;
    private final EncryptionService encryptionService;
    private final ProvisioningProperties props;
    private final ResourcePatternResolver resourceResolver;

    @Override
    public void run(ApplicationArguments args) {
        List<Map<String, Object>> tenants = jdbc.queryForList("""
                SELECT t.id, ts.db_name, ts.db_host, ts.db_port, ts.db_user, ts.db_password
                FROM tenants t
                JOIN tenant_settings ts ON ts.tenant_id = t.id
                WHERE t.activated_at IS NOT NULL AND t.deleted_at IS NULL
                """);

        for (Map<String, Object> row : tenants) {
            try {
                String tenantId = row.get("id").toString();
                String dbName = (String) row.get("db_name");
                String dbHost = (String) row.get("db_host");
                String dbPort = (String) row.get("db_port");
                String dbUser = (String) row.get("db_user");
                String encryptedPassword = (String) row.get("db_password");
                String dbPassword = encryptionService.decrypt(encryptedPassword);
                migrateTenant(tenantId, dbHost, dbPort, dbName, dbUser, dbPassword);
            } catch (Exception e) {
                String id = row.get("id") != null ? row.get("id").toString() : "unknown";
                String dbName = (String) row.get("db_name");
                log.error("Failed to migrate tenant {} ({}): {}", id, dbName, e.getMessage());
            }
        }
    }

    private void migrateTenant(String tenantId, String host, String port, String dbName,
                                String user, String password) {
        String url = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
            Resource[] resources = resourceResolver.getResources("classpath:" + MIGRATION_PATH + "*.sql");
            List.of(resources).stream()
                    .sorted(Comparator.comparing(Resource::getFilename))
                    .forEach(script -> {
                        try {
                            ScriptUtils.executeSqlScript(conn, script);
                            log.info("Executed {} on tenant {} ({})", script.getFilename(), tenantId, dbName);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to execute " + script.getFilename(), e);
                        }
                    });
            log.info("Migrated tenant {} ({})", tenantId, dbName);
        } catch (SQLException | IOException e) {
            throw new TenantProvisioningException(
                    "Failed to migrate tenant DB: " + dbName, e);
        }
    }
}
