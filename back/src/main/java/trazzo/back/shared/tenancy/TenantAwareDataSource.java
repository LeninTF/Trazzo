package trazzo.back.shared.tenancy;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * Wraps the application's single physical DataSource so every connection checkout is
 * pinned to the current request's tenant schema via {@code SET search_path}. This makes
 * both JPA (EntityManager) and JdbcTemplate consumers tenant-aware transparently, since
 * they ultimately all call {@link #getConnection()} on the same bean.
 */
public class TenantAwareDataSource extends DelegatingDataSource {

    // Schema names are derived from sanitized subdomains (see TenantSchemaProvisioningAdapter);
    // this mirrors that allow-list. SET search_path cannot use a JDBC bind parameter, so the
    // value is validated here before being concatenated into SQL.
    private static final Pattern VALID_SCHEMA = Pattern.compile("^[a-z0-9_]+$");

    public TenantAwareDataSource(DataSource targetDataSource) {
        super(targetDataSource);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return withSchema(super.getConnection());
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return withSchema(super.getConnection(username, password));
    }

    // Schema is validated against VALID_SCHEMA immediately below before being concatenated;
    // SET search_path cannot use a JDBC bind parameter for the identifier.
    @SuppressWarnings("java:S2077")
    private Connection withSchema(Connection connection) throws SQLException {
        // SET search_path is PostgreSQL-specific syntax; other engines (e.g. the H2
        // in-memory database used by tests) don't support it and don't need tenant
        // schema routing in the first place, so skip it for non-PostgreSQL connections.
        if (!"PostgreSQL".equals(connection.getMetaData().getDatabaseProductName())) {
            return connection;
        }

        String schema = TenantContext.get();
        if (!VALID_SCHEMA.matcher(schema).matches()) {
            connection.close();
            throw new SQLException("Invalid tenant schema name: " + schema);
        }
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET search_path TO \"" + schema + "\", public");
        }
        return connection;
    }
}
