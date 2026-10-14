package trazzo.back.saasglobal.infrastructure.adapters.out.provisioning;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import javax.sql.DataSource;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class TenantFlywayMigratorTest {

    private final DataSource dataSource = mock(DataSource.class);
    private final TenantFlywayMigrator migrator = new TenantFlywayMigrator(dataSource);

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "Tenant_Acme", "tenant acme", "tenant_acme\"; DROP SCHEMA public CASCADE; --", "tenant-acme"})
    void rejectsUnsafeSchemaNamesWithoutTouchingTheDataSource(String schemaName) {
        assertThrows(TenantProvisioningException.class, () -> migrator.migrate(schemaName));

        verifyNoInteractions(dataSource);
    }
}
