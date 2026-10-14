package trazzo.back.saasglobal.infrastructure.adapters.out.provisioning;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class TenantSchemaMigratorTest {

    @Mock JdbcTemplate jdbc;
    @Mock TenantFlywayMigrator flywayMigrator;

    private TenantSchemaMigrator migrator;

    @BeforeEach
    void setUp() {
        migrator = new TenantSchemaMigrator(jdbc, flywayMigrator, false);
    }

    private static Map<String, Object> tenantRow(String schemaName) {
        Map<String, Object> row = new HashMap<>();
        row.put("id", UUID.randomUUID());
        row.put("schema_name", schemaName);
        return row;
    }

    @Test
    void shouldDoNothingWhenNoTenants() {
        when(jdbc.queryForList(anyString())).thenReturn(List.of());

        migrator.run(mock(ApplicationArguments.class));

        verify(jdbc).queryForList(anyString());
        verifyNoInteractions(flywayMigrator);
        assertEquals(0, migrator.failedTenants());
    }

    @Test
    void shouldMigrateEveryActivatedTenant() {
        when(jdbc.queryForList(anyString())).thenReturn(List.of(tenantRow("tenant_demo"), tenantRow("tenant_acme")));
        when(flywayMigrator.migrate(anyString())).thenReturn(0);

        migrator.run(mock(ApplicationArguments.class));

        verify(flywayMigrator).migrate("tenant_demo");
        verify(flywayMigrator).migrate("tenant_acme");
        assertEquals(0, migrator.failedTenants());
    }

    @Test
    void shouldLogErrorAndKeepMigratingTheOthersWhenOneTenantFails() {
        when(jdbc.queryForList(anyString())).thenReturn(List.of(tenantRow("tenant_broken"), tenantRow("tenant_ok")));
        when(flywayMigrator.migrate("tenant_broken")).thenThrow(new IllegalStateException("boom"));

        assertDoesNotThrow(() -> migrator.run(mock(ApplicationArguments.class)));

        // A failure on one tenant must not stop the loop from reaching the next one.
        verify(flywayMigrator).migrate("tenant_ok");
        assertEquals(1, migrator.failedTenants());
    }

    @Test
    void shouldAbortStartupWhenFailOnErrorIsEnabled() {
        migrator = new TenantSchemaMigrator(jdbc, flywayMigrator, true);
        when(jdbc.queryForList(anyString())).thenReturn(List.of(tenantRow("tenant_broken"), tenantRow("tenant_ok")));
        when(flywayMigrator.migrate("tenant_broken")).thenThrow(new IllegalStateException("boom"));

        assertThrows(TenantProvisioningException.class, () -> migrator.run(mock(ApplicationArguments.class)));

        verify(flywayMigrator, never()).migrate("tenant_ok");
        assertEquals(1, migrator.failedTenants());
    }

    @Test
    void shouldResetTheFailureCountOnEachRun() {
        when(jdbc.queryForList(anyString())).thenReturn(List.of(tenantRow("tenant_broken")));
        when(flywayMigrator.migrate("tenant_broken")).thenThrow(new IllegalStateException("boom"));
        migrator.run(mock(ApplicationArguments.class));
        assertEquals(1, migrator.failedTenants());

        when(jdbc.queryForList(anyString())).thenReturn(List.of());
        migrator.run(mock(ApplicationArguments.class));

        assertEquals(0, migrator.failedTenants());
    }

    @Test
    void shouldTreatATenantWithoutSchemaNameAsAFailure() {
        Map<String, Object> row = new HashMap<>();
        row.put("id", null);
        row.put("schema_name", null);
        when(jdbc.queryForList(anyString())).thenReturn(List.of(row));
        when(flywayMigrator.migrate(null)).thenThrow(new TenantProvisioningException("Invalid schema name: null", null));

        assertDoesNotThrow(() -> migrator.run(mock(ApplicationArguments.class)));

        assertEquals(1, migrator.failedTenants());
    }
}
