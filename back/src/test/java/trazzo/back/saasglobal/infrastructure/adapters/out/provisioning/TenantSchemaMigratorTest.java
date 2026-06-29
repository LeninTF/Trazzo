package trazzo.back.saasglobal.infrastructure.adapters.out.provisioning;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import trazzo.back.saasglobal.infrastructure.config.ProvisioningProperties;
import trazzo.back.saasglobal.infrastructure.security.EncryptionService;

@ExtendWith(MockitoExtension.class)
class TenantSchemaMigratorTest {

    @Mock JdbcTemplate jdbc;
    @Mock EncryptionService encryptionService;
    @Mock ResourcePatternResolver resourceResolver;

    private TenantSchemaMigrator migrator;

    @BeforeEach
    void setUp() {
        migrator = new TenantSchemaMigrator(jdbc, encryptionService,
                new ProvisioningProperties("url", "user", "pass", "host", "port"),
                resourceResolver);
    }

    @Test
    void shouldDoNothingWhenNoTenants() {
        when(jdbc.queryForList(anyString())).thenReturn(List.of());

        migrator.run(mock(ApplicationArguments.class));

        verify(jdbc).queryForList(anyString());
        verifyNoInteractions(encryptionService);
    }

    @Test
    void shouldLogErrorWhenMigrationFails() {
        Map<String, Object> row = new HashMap<>();
        row.put("id", UUID.randomUUID());
        row.put("db_name", "testdb");
        row.put("db_host", "localhost");
        row.put("db_port", "5432");
        row.put("db_user", "admin");
        row.put("db_password", "encrypted");

        when(jdbc.queryForList(anyString())).thenReturn(List.of(row));
        when(encryptionService.decrypt("encrypted")).thenReturn("decrypted");

        migrator.run(mock(ApplicationArguments.class));

        verify(encryptionService).decrypt("encrypted");
    }

    @Test
    void shouldMigrateSuccessfully() throws Exception {
        UUID tenantId = UUID.randomUUID();
        Map<String, Object> row = new HashMap<>();
        row.put("id", tenantId);
        row.put("db_name", "testdb");
        row.put("db_host", "localhost");
        row.put("db_port", "5432");
        row.put("db_user", "admin");
        row.put("db_password", "encrypted");

        when(jdbc.queryForList(anyString())).thenReturn(List.of(row));
        when(encryptionService.decrypt("encrypted")).thenReturn("decrypted");

        try (MockedStatic<DriverManager> driverManagerMock = mockStatic(DriverManager.class)) {
            Connection conn = mock(Connection.class);
            driverManagerMock.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(conn);
            when(resourceResolver.getResources(anyString())).thenReturn(new Resource[0]);

            migrator.run(mock(ApplicationArguments.class));

            driverManagerMock.verify(() -> DriverManager.getConnection(
                    startsWith("jdbc:postgresql://"), eq("admin"), eq("decrypted")));
            verify(resourceResolver).getResources(contains("db/tenant/migration/"));
            verify(encryptionService).decrypt("encrypted");
        }
    }
}
