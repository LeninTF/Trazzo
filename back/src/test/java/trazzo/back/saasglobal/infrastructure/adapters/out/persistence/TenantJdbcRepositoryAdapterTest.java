package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import trazzo.back.saasglobal.domain.model.multitenancy.Tenant;
import trazzo.back.saasglobal.domain.model.multitenancy.TenantSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantJdbcRepositoryAdapterTest {

    @Mock JdbcTemplate jdbc;
    @InjectMocks TenantJdbcRepositoryAdapter adapter;

    private static TenantSettings settings() {
        return TenantSettings.of("t-1", "localhost", "5432", "db", "user", "pass");
    }

    @Test
    void save_returnsSameTenant() {
        var tenant = Tenant.createTrial("acme", 1, null, settings(), null);

        var result = adapter.save(tenant);

        assertSame(tenant, result);
    }

    @Test
    void save_withBrandingReturnsSameTenant() {
        var tenant = Tenant.createTrial("acme", 1, null, settings(), null);
        tenant.assignBranding(trazzo.back.saasglobal.domain.model.multitenancy.TenantBranding
                .of(null, "logo", "s", "#a", "#b"));

        var result = adapter.save(tenant);

        assertSame(tenant, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findById_returnsEmptyWhenNotFound() {
        when(jdbc.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());

        Optional<Tenant> result = adapter.findById("missing-id");

        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findBySubDomain_returnsEmptyWhenNotFound() {
        when(jdbc.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());

        Optional<Tenant> result = adapter.findBySubDomain("unknown");

        assertTrue(result.isEmpty());
    }

    @Test
    void existsBySubDomain_returnsTrueWhenCountPositive() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("acme"))).thenReturn(1);

        assertTrue(adapter.existsBySubDomain("acme"));
    }

    @Test
    void existsBySubDomain_returnsFalseWhenCountZero() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("acme"))).thenReturn(0);

        assertFalse(adapter.existsBySubDomain("acme"));
    }

    @Test
    void existsBySubDomain_returnsFalseWhenNullReturned() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("acme"))).thenReturn(null);

        assertFalse(adapter.existsBySubDomain("acme"));
    }
}
