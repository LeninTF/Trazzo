package trazzo.back.shared.tenancy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantAwareDataSourceTest {

    @AfterEach
    void cleanup() {
        TenantContext.clear();
    }

    private static void stubDatabaseProductName(Connection connection, String productName) throws SQLException {
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(metaData.getDatabaseProductName()).thenReturn(productName);
        when(connection.getMetaData()).thenReturn(metaData);
    }

    @Test
    void getConnection_setsSearchPathForCurrentTenant() throws SQLException {
        DataSource delegate = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(delegate.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        stubDatabaseProductName(connection, "PostgreSQL");

        TenantContext.set("tenant_acme");
        var dataSource = new TenantAwareDataSource(delegate);

        Connection result = dataSource.getConnection();

        verify(statement).execute("SET search_path TO \"tenant_acme\", public");
        assertEquals(connection, result);
    }

    @Test
    void getConnection_defaultsToPublicWhenNoTenantSet() throws SQLException {
        DataSource delegate = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(delegate.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        stubDatabaseProductName(connection, "PostgreSQL");

        var dataSource = new TenantAwareDataSource(delegate);

        dataSource.getConnection();

        verify(statement).execute("SET search_path TO \"public\", public");
    }

    @Test
    void getConnection_rejectsInvalidSchemaAndClosesConnection() throws SQLException {
        DataSource delegate = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(delegate.getConnection()).thenReturn(connection);
        stubDatabaseProductName(connection, "PostgreSQL");

        TenantContext.set("tenant_acme\"; DROP SCHEMA public CASCADE; --");
        var dataSource = new TenantAwareDataSource(delegate);

        assertThrows(SQLException.class, dataSource::getConnection);
        verify(connection).close();
        verify(connection, never()).createStatement();
    }

    @Test
    void getConnectionWithCredentials_alsoSetsSearchPath() throws SQLException {
        DataSource delegate = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(delegate.getConnection(eq("user"), eq("pass"))).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        stubDatabaseProductName(connection, "PostgreSQL");

        TenantContext.set("tenant_acme");
        var dataSource = new TenantAwareDataSource(delegate);

        dataSource.getConnection("user", "pass");

        verify(statement).execute("SET search_path TO \"tenant_acme\", public");
    }

    @Test
    void getConnection_skipsSearchPathForNonPostgresConnections() throws SQLException {
        DataSource delegate = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(delegate.getConnection()).thenReturn(connection);
        stubDatabaseProductName(connection, "H2");

        TenantContext.set("tenant_acme");
        var dataSource = new TenantAwareDataSource(delegate);

        Connection result = dataSource.getConnection();

        verify(connection, never()).createStatement();
        assertEquals(connection, result);
    }
}
