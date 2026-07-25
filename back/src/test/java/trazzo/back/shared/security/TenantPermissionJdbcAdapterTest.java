package trazzo.back.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import trazzo.back.shared.tenancy.TenantContext;

class TenantPermissionJdbcAdapterTest {

    private JdbcTemplate jdbc;
    private TenantPermissionJdbcAdapter adapter;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        adapter = new TenantPermissionJdbcAdapter(jdbc);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void findPermissionCodes_returnsEmpty_whenSchemaIsPublic() {
        TenantContext.set(null);

        var codes = adapter.findPermissionCodesByMasterUserId(UUID.randomUUID());

        assertThat(codes).isEmpty();
        verify(jdbc, never()).queryForList(anyString(), eq(String.class), anyString());
    }

    @Test
    void findPermissionCodes_returnsEmpty_whenTenantContextExplictlyPublic() {
        TenantContext.set("public");

        var codes = adapter.findPermissionCodesByMasterUserId(UUID.randomUUID());

        assertThat(codes).isEmpty();
        verify(jdbc, never()).queryForList(anyString(), eq(String.class), anyString());
    }

    @Test
    void findPermissionCodes_returnsTenantPermissions_whenTenantSchemaIsSet() {
        TenantContext.set("tenant_acme");
        var masterUserId = UUID.randomUUID();
        var expectedCodes = List.of("incidents.crear", "incidents.aprobar");
        when(jdbc.queryForList(anyString(), eq(String.class), eq(masterUserId.toString())))
                .thenReturn(expectedCodes);

        var codes = adapter.findPermissionCodesByMasterUserId(masterUserId);

        assertThat(codes).containsExactly("incidents.crear", "incidents.aprobar");
        verify(jdbc).queryForList(anyString(), eq(String.class), eq(masterUserId.toString()));
    }

    @Test
    void findPermissionCodes_returnsEmpty_whenJdbcReturnsEmpty() {
        TenantContext.set("tenant_demo");
        var masterUserId = UUID.randomUUID();
        when(jdbc.queryForList(anyString(), eq(String.class), eq(masterUserId.toString())))
                .thenReturn(List.of());

        var codes = adapter.findPermissionCodesByMasterUserId(masterUserId);

        assertThat(codes).isEmpty();
    }

    @Test
    void findPermissionCodes_usesTenantContextSetBeforeQuery() {
        TenantContext.set("tenant_demo");
        var masterUserId = UUID.randomUUID();
        when(jdbc.queryForList(anyString(), eq(String.class), eq(masterUserId.toString())))
                .thenReturn(List.of("perm-one"));

        adapter.findPermissionCodesByMasterUserId(masterUserId);

        // The adapter does not clear TenantContext; the caller (filter) owns the lifecycle.
        assertThat(TenantContext.get()).isEqualTo("tenant_demo");
    }
}
