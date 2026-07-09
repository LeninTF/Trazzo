package trazzo.back.saasglobal.infrastructure.adapters.out.provisioning;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class TenantSchemaMigratorTest {

    @Mock JdbcTemplate jdbc;
    @Mock DataSource rawDataSource;
    @Mock ResourcePatternResolver resourceResolver;
    @Mock Connection connection;
    @Mock Statement statement;

    private TenantSchemaMigrator migrator;

    @BeforeEach
    void setUp() {
        migrator = new TenantSchemaMigrator(jdbc, rawDataSource, resourceResolver);
    }

    @Test
    void shouldDoNothingWhenNoTenants() {
        when(jdbc.queryForList(anyString())).thenReturn(List.of());

        migrator.run(mock(ApplicationArguments.class));

        verify(jdbc).queryForList(anyString());
        verifyNoInteractions(rawDataSource);
    }

    @Test
    void shouldLogErrorAndSkipWhenSchemaNameInvalid() {
        Map<String, Object> row = new HashMap<>();
        row.put("id", UUID.randomUUID());
        row.put("schema_name", "invalid schema; DROP TABLE x");

        when(jdbc.queryForList(anyString())).thenReturn(List.of(row));

        assertDoesNotThrow(() -> migrator.run(mock(ApplicationArguments.class)));

        verifyNoInteractions(rawDataSource);
    }

    @Test
    void shouldMigrateSuccessfully() throws Exception {
        UUID tenantId = UUID.randomUUID();
        Map<String, Object> row = new HashMap<>();
        row.put("id", tenantId);
        row.put("schema_name", "tenant_demo");

        when(jdbc.queryForList(anyString())).thenReturn(List.of(row));
        when(rawDataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(resourceResolver.getResources(anyString())).thenReturn(new Resource[0]);

        migrator.run(mock(ApplicationArguments.class));

        verify(statement).execute("SET search_path TO \"tenant_demo\"");
        verify(resourceResolver).getResources(contains("db/tenant/migration/"));
    }
}
