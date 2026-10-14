package trazzo.back.saasglobal.infrastructure.adapters.out.provisioning;

import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Applies {@code db/tenant/migration/V*.sql} to one tenant schema with Flyway, tracking what
 * was applied in {@code flyway_tenant_history} inside that same schema. Schemas created before
 * the history table existed (they already contain the V1 tables) are baselined at version 0 and
 * then receive every pending script exactly once.
 *
 * Uses the raw DataSource, like {@link TenantSchemaProvisioningAdapter}, so it does not depend
 * on the per-request TenantContext. Flyway points {@code search_path} at the schema being
 * migrated, which is what {@code current_schema()} in the scripts relies on.
 */
@Component
public class TenantFlywayMigrator {

    static final String LOCATION = "classpath:db/tenant/migration";
    static final String HISTORY_TABLE = "flyway_tenant_history";
    private static final Pattern VALID_SCHEMA = Pattern.compile("^[a-z0-9_]+$");

    private final DataSource rawDataSource;

    public TenantFlywayMigrator(@Qualifier("rawDataSource") DataSource rawDataSource) {
        this.rawDataSource = rawDataSource;
    }

    /** Migrates {@code schemaName} and returns how many migrations were applied (0 when up to date). */
    public int migrate(String schemaName) {
        if (schemaName == null || !VALID_SCHEMA.matcher(schemaName).matches()) {
            throw new TenantProvisioningException("Invalid schema name: " + schemaName, null);
        }
        return Flyway.configure()
                .dataSource(rawDataSource)
                .schemas(schemaName)
                .defaultSchema(schemaName)
                .createSchemas(false)
                .locations(LOCATION)
                .table(HISTORY_TABLE)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load()
                .migrate()
                .migrationsExecuted;
    }
}
