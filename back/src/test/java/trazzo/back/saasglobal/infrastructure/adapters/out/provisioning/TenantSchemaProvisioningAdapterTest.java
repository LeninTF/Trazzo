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
        adapter = new TenantSchemaProvisioningAdapter(rawDataSource);
    }

    @Test
    void provisionNew_createsSchemaDerivedFromSubDomainAndRunsScript() throws SQLException {
        wireHappyPathConnection();

        TenantSettings result = adapter.provisionNew("t-1", "Acme");

        assertEquals("t-1", result.getTenantId());
        assertEquals("tenant_acme", result.getSchemaName());
        verify(statement).execute("CREATE SCHEMA IF NOT EXISTS \"tenant_acme\"");
        verify(statement).execute("SET search_path TO \"tenant_acme\"");
    }

    @Test
    void provisionExisting_provisionsGivenSchemaName() throws SQLException {
        wireHappyPathConnection();

        adapter.provisionExisting(TenantSettings.of("t-1", "tenant_acme"));

        verify(statement).execute("CREATE SCHEMA IF NOT EXISTS \"tenant_acme\"");
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

    private void verifyNoDataSourceInteraction() {
        try {
            verify(rawDataSource, never()).getConnection();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }
}
