package trazzo.back.saasglobal.infrastructure.adapters.out.provisioning;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import trazzo.back.saasglobal.domain.model.multitenancy.TenantSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantSchemaProvisioningAdapterTest {

    @Mock DataSource rawDataSource;
    @Mock Connection connection;
    @Mock Statement statement;

    private TenantSchemaProvisioningAdapter adapter;

    private void wireHappyPathConnection() throws SQLException {
        when(rawDataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        // ScriptUtils.executeSqlScript loops on getMoreResults()/getUpdateCount() after each
        // statement until getUpdateCount() reports -1 (JDBC's "no more results" sentinel); an
        // unstubbed mock defaults to 0, which never satisfies that check and spins forever.
        when(statement.getUpdateCount()).thenReturn(-1);
        adapter = new TenantSchemaProvisioningAdapter(rawDataSource);
    }

    @Test
    void provisionNew_createsSchemaDerivedFromSubDomainAndRunsScript() throws SQLException {
        wireHappyPathConnection();

        TenantSettings result = adapter.provisionNew("t-1", "Acme");

        assertEquals("t-1", result.getTenantId());
        assertEquals("tenant_acme", result.getSchemaName());
        verify(statement).execute("CREATE SCHEMA \"tenant_acme\"");
        verify(statement).execute("SET search_path TO \"tenant_acme\", public");
    }

    @Test
    void provisionExisting_provisionsGivenSchemaName() throws SQLException {
        wireHappyPathConnection();

        adapter.provisionExisting(TenantSettings.of("t-1", "tenant_acme"));

        verify(statement).execute("CREATE SCHEMA \"tenant_acme\"");
    }

    @Test
    void provisionExisting_failsLoudlyOnSchemaNameCollisionInsteadOfReusingIt() throws SQLException {
        // No IF NOT EXISTS: if two tenants ever derive the same schema name, provisioning
        // must fail instead of silently reusing (and sharing) the first tenant's schema.
        when(rawDataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute("CREATE SCHEMA \"tenant_acme\""))
                .thenThrow(new SQLException("schema \"tenant_acme\" already exists", "42P06"));
        adapter = new TenantSchemaProvisioningAdapter(rawDataSource);
        var settings = TenantSettings.of("t-1", "tenant_acme");

        assertThrows(TenantProvisioningException.class, () -> adapter.provisionExisting(settings));

        // The schema wasn't created by this call (it already existed), so it must not be
        // dropped — that would destroy the other tenant's schema on a name collision.
        verify(statement, never()).execute("DROP SCHEMA IF EXISTS \"tenant_acme\" CASCADE");
    }

    @Test
    void provisionExisting_dropsSchemaWhenPopulatingItFailsAfterCreation() throws SQLException {
        when(rawDataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.execute("SET search_path TO \"tenant_acme\", public"))
                .thenThrow(new SQLException("boom"));
        adapter = new TenantSchemaProvisioningAdapter(rawDataSource);
        var settings = TenantSettings.of("t-1", "tenant_acme");

        assertThrows(TenantProvisioningException.class, () -> adapter.provisionExisting(settings));

        verify(statement).execute("CREATE SCHEMA \"tenant_acme\"");
        verify(statement).execute("DROP SCHEMA IF EXISTS \"tenant_acme\" CASCADE");
    }

    @Test
    void provisionExisting_wrapsSqlExceptionAsProvisioningException() throws SQLException {
        when(rawDataSource.getConnection()).thenThrow(new SQLException("boom"));
        adapter = new TenantSchemaProvisioningAdapter(rawDataSource);
        var settings = TenantSettings.of("t-1", "tenant_acme");

        assertThrows(TenantProvisioningException.class, () -> adapter.provisionExisting(settings));
    }

    @Test
    void deprovision_dropsSchemaCascade() throws SQLException {
        wireHappyPathConnection();

        adapter.deprovision("tenant_acme");

        verify(statement).execute("DROP SCHEMA IF EXISTS \"tenant_acme\" CASCADE");
    }

    @Test
    void deprovision_swallowsFailuresBestEffort() throws SQLException {
        when(rawDataSource.getConnection()).thenThrow(new SQLException("boom"));
        adapter = new TenantSchemaProvisioningAdapter(rawDataSource);

        assertDoesNotThrow(() -> adapter.deprovision("tenant_acme"));
    }

    @Test
    void deprovision_rejectsUnsafeIdentifierWithoutTouchingDataSource() {
        adapter = new TenantSchemaProvisioningAdapter(rawDataSource);

        adapter.deprovision("tenant_acme\"; DROP SCHEMA public CASCADE; --");

        verifyNoDataSourceInteraction();
    }

    @Test
    void provisionExisting_rejectsUnsafeSchemaNameWithoutTouchingDataSource() {
        adapter = new TenantSchemaProvisioningAdapter(rawDataSource);
        var maliciousSettings = TenantSettings.of("t-1", "tenant_acme; DROP TABLE users");

        assertThrows(TenantProvisioningException.class,
                () -> adapter.provisionExisting(maliciousSettings));
        verifyNoDataSourceInteraction();
    }

    @Test
    void provisionNew_failsWhenCreateSchemaThrows() throws SQLException {
        when(rawDataSource.getConnection()).thenThrow(new SQLException("boom"));
        adapter = new TenantSchemaProvisioningAdapter(rawDataSource);

        assertThrows(TenantProvisioningException.class,
                () -> adapter.provisionNew("t-1", "Acme"));
    }

    @Test
    void recreateSchema_dropsSchema() throws SQLException {
        when(rawDataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        adapter = new TenantSchemaProvisioningAdapter(rawDataSource);

        adapter.recreateSchema("tenant_acme");

        verify(statement).execute("DROP SCHEMA IF EXISTS \"tenant_acme\" CASCADE");
    }

    @Test
    void recreateSchema_throwsOnSqlException() throws SQLException {
        when(rawDataSource.getConnection()).thenThrow(new SQLException("boom"));
        adapter = new TenantSchemaProvisioningAdapter(rawDataSource);

        assertThrows(TenantProvisioningException.class,
                () -> adapter.recreateSchema("tenant_acme"));
    }

    @Test
    void recreateSchema_rejectsUnsafeIdentifier() {
        adapter = new TenantSchemaProvisioningAdapter(rawDataSource);

        assertThrows(TenantProvisioningException.class,
                () -> adapter.recreateSchema("tenant_acme; DROP TABLE users"));

        verifyNoDataSourceInteraction();
    }

    private void verifyNoDataSourceInteraction() {
        try {
            verify(rawDataSource, never()).getConnection();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
