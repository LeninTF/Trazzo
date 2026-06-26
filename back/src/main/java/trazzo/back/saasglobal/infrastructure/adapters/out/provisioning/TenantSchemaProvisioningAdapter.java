package trazzo.back.saasglobal.infrastructure.adapters.out.provisioning;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import trazzo.back.saasglobal.application.port.out.TenantSchemaProvisioningPort;
import trazzo.back.saasglobal.domain.model.multitenancy.TenantSettings;
import trazzo.back.saasglobal.infrastructure.config.ProvisioningProperties;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantSchemaProvisioningAdapter implements TenantSchemaProvisioningPort {

    private static final String SCHEMA_SCRIPT = "db/tenant/schema.sql";
    private static final int MAX_SAFE_LENGTH = 40;

    private final ProvisioningProperties props;

    /**
     * PAID flow: creates a new isolated PostgreSQL database + user, runs the schema script,
     * and returns the generated connection settings for the new tenant.
     * The dbName and dbUser include a tenantId-derived suffix to prevent collisions
     * when different subdomains sanitize to the same identifier.
     */
    @Override
    public TenantSettings provisionNew(String tenantId, String subDomain) {
        String safe = subDomain.toLowerCase().replaceAll("[^a-z0-9]", "_");
        String truncated = safe.length() > MAX_SAFE_LENGTH ? safe.substring(0, MAX_SAFE_LENGTH) : safe;
        String tenantSuffix = tenantId.replace("-", "").substring(0, 8);
        String dbName = "tenant_" + truncated + "_" + tenantSuffix;
        String dbUser = "user_" + truncated + "_" + tenantSuffix;
        String dbPassword = generatePassword();

        createDatabaseAndUser(dbName, dbUser, dbPassword);

        String tenantJdbcUrl = buildJdbcUrl(props.dbHost(), props.dbPort(), dbName);
        runSchemaScript(tenantJdbcUrl, props.adminUsername(), props.adminPassword());
        grantPrivilegesToUser(tenantJdbcUrl, dbUser);

        return TenantSettings.of(tenantId, props.dbHost(), props.dbPort(), dbName, dbUser, dbPassword);
    }

    /**
     * TRIAL flow: runs the schema script against an existing database using provided credentials.
     */
    @Override
    public void provisionExisting(TenantSettings settings) {
        String url = buildJdbcUrl(settings.getDbHost(), settings.getDbPort(), settings.getDbName());
        runSchemaScript(url, settings.getDbUser(), settings.getDbPassword());
    }

    /**
     * Best-effort compensation: drops database and user if provisioning succeeded
     * but the master transaction failed afterward. Errors are logged, not rethrown.
     */
    @Override
    @SuppressWarnings("java:S2077")
    public void deprovision(String dbName, String dbUser) {
        try {
            validateIdentifier(dbName);
            validateIdentifier(dbUser);
            try (Connection conn = DriverManager.getConnection(
                    props.adminUrl(), props.adminUsername(), props.adminPassword())) {
                conn.setAutoCommit(true);
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("DROP DATABASE IF EXISTS \"" + dbName + "\"");
                    stmt.execute("DROP USER IF EXISTS \"" + dbUser + "\"");
                }
            }
        } catch (Exception e) {
            log.warn("Best-effort deprovision failed for DB '{}', user '{}': {}", dbName, dbUser, e.getMessage());
        }
    }

    // DDL statements cannot use JDBC parameters — identifiers are sanitized to [a-z0-9_] only
    @SuppressWarnings("java:S2077")
    private void createDatabaseAndUser(String dbName, String dbUser, String dbPassword) {
        validateIdentifier(dbName);
        validateIdentifier(dbUser);
        try (Connection conn = DriverManager.getConnection(props.adminUrl(), props.adminUsername(), props.adminPassword())) {
            conn.setAutoCommit(true); // CREATE DATABASE cannot run inside a transaction
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE DATABASE \"" + dbName + "\"");
                stmt.execute("CREATE USER \"" + dbUser + "\" WITH ENCRYPTED PASSWORD '" + dbPassword + "'");
                stmt.execute("GRANT ALL PRIVILEGES ON DATABASE \"" + dbName + "\" TO \"" + dbUser + "\"");
            }
        } catch (SQLException e) {
            throw new TenantProvisioningException("Failed to create database/user for tenant: " + dbName, e);
        }
    }

    private void runSchemaScript(String jdbcUrl, String user, String password) {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, user, password)) {
            ScriptUtils.executeSqlScript(conn, new ClassPathResource(SCHEMA_SCRIPT));
        } catch (SQLException e) {
            throw new TenantProvisioningException("Failed to run tenant schema script on: " + jdbcUrl, e);
        }
    }

    @SuppressWarnings("java:S2077")
    private void grantPrivilegesToUser(String tenantJdbcUrl, String dbUser) {
        validateIdentifier(dbUser);
        try (Connection conn = DriverManager.getConnection(tenantJdbcUrl, props.adminUsername(), props.adminPassword())) {
            conn.setAutoCommit(true);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("GRANT ALL ON ALL TABLES IN SCHEMA public TO \"" + dbUser + "\"");
                stmt.execute("GRANT ALL ON ALL SEQUENCES IN SCHEMA public TO \"" + dbUser + "\"");
                stmt.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO \"" + dbUser + "\"");
                stmt.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO \"" + dbUser + "\"");
            }
        } catch (SQLException e) {
            throw new TenantProvisioningException("Failed to grant privileges to user: " + dbUser, e);
        }
    }

    private static String buildJdbcUrl(String host, String port, String dbName) {
        return "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
    }

    private static String generatePassword() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private static void validateIdentifier(String value) {
        if (value == null || !value.matches("[a-z0-9_]+")) {
            throw new TenantProvisioningException(
                    "Identifier contains unsafe characters: " + value, null);
        }
    }
}
