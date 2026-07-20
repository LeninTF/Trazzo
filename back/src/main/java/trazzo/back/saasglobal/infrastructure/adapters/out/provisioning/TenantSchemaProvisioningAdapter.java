package trazzo.back.saasglobal.infrastructure.adapters.out.provisioning;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import trazzo.back.saasglobal.application.port.out.TenantSchemaProvisioningPort;
import trazzo.back.saasglobal.domain.model.multitenancy.TenantSettings;

/**
 * Provisions a per-tenant PostgreSQL schema within the single physical application
 * database (as opposed to a separate physical database per tenant). Uses the raw,
 * un-wrapped DataSource directly so it can manage {@code search_path} for the schema
 * being provisioned without depending on (or mutating) the ambient per-request
 * {@code TenantContext}.
 */
@Slf4j
@Component
public class TenantSchemaProvisioningAdapter implements TenantSchemaProvisioningPort {

    private static final String SCHEMA_SCRIPT = "db/tenant/V1__tenant_db.sql";
    private static final Pattern VALID_SCHEMA = Pattern.compile("^[a-z0-9_]+$");

    private final DataSource rawDataSource;

    public TenantSchemaProvisioningAdapter(@Qualifier("rawDataSource") DataSource rawDataSource) {
        this.rawDataSource = rawDataSource;
    }

    @Override
    public TenantSettings provisionNew(String tenantId, String subDomain) {
        String schemaName = TenantSettings.deriveSchemaName(subDomain);
        provisionSchema(schemaName);
        return TenantSettings.of(tenantId, schemaName);
    }

    @Override
    public void provisionExisting(TenantSettings settings) {
        provisionSchema(settings.getSchemaName());
    }

    /**
     * Best-effort compensation: drops the schema if provisioning succeeded but the
     * master transaction failed afterward. Errors are logged, not rethrown.
     */
    @Override
    @SuppressWarnings("java:S2077")
    public void deprovision(String schemaName) {
        try {
            validateIdentifier(schemaName);
            try (Connection conn = rawDataSource.getConnection();
                    Statement stmt = conn.createStatement()) {
                stmt.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
            }
        } catch (Exception e) {
            log.warn("Best-effort deprovision failed for schema '{}': {}", schemaName, e.getMessage());
        }
    }

    // Identifier is sanitized to [a-z0-9_] by validateIdentifier() before this ever runs.
    @SuppressWarnings("java:S2077")
    private void provisionSchema(String schemaName) {
        validateIdentifier(schemaName);
        createSchema(schemaName);
        try (Connection conn = rawDataSource.getConnection()) {
            try (Statement stmt = conn.createStatement()) {
                // public is functionally redundant for gen_random_uuid() (pg_catalog-builtin
                // since PG13, always implicitly searched first) but kept for defense in depth
                // and to match TenantAwareDataSource's runtime search_path exactly.
                stmt.execute("SET search_path TO \"" + schemaName + "\", public");
            }
            ScriptUtils.executeSqlScript(conn, new ClassPathResource(SCHEMA_SCRIPT));
        } catch (Exception e) {
            // The schema was created by us (createSchema() above already succeeded), so a
            // failure past this point must not leave it behind half-populated — that would
            // block retrying the same subDomain (CREATE SCHEMA no longer tolerates a collision).
            dropSchemaBestEffort(schemaName);
            throw new TenantProvisioningException("Failed to provision schema: " + schemaName, e);
        }
    }

    @SuppressWarnings("java:S2077")
    private void createSchema(String schemaName) {
        try (Connection conn = rawDataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA \"" + schemaName + "\"");
        } catch (SQLException e) {
            throw new TenantProvisioningException("Failed to provision schema: " + schemaName, e);
        }
    }

    /**
     * Drops and recreates the schema from scratch. Used by {@code TenantDataSeeder}
     * to recover from orphaned schemas left behind by a previous failed provisioning
     * attempt during local development.
     */
    @SuppressWarnings("java:S2077")
    public void recreateSchema(String schemaName) {
        validateIdentifier(schemaName);
        try (Connection conn = rawDataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
            stmt.execute("CREATE SCHEMA \"" + schemaName + "\"");
        } catch (SQLException e) {
            throw new TenantProvisioningException("Failed to recreate schema: " + schemaName, e);
        }
    }

    @SuppressWarnings("java:S2077")
    private void dropSchemaBestEffort(String schemaName) {
        try (Connection conn = rawDataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
        } catch (SQLException dropEx) {
            log.warn("Failed to drop orphaned schema '{}' after provisioning failure: {}",
                    schemaName, dropEx.getMessage());
        }
    }

    private static void validateIdentifier(String value) {
        if (value == null || !VALID_SCHEMA.matcher(value).matches()) {
            throw new TenantProvisioningException(
                    "Identifier contains unsafe characters: " + value, null);
        }
    }
}
