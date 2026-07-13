package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import trazzo.back.saasglobal.domain.model.multitenancy.Tenant;
import trazzo.back.saasglobal.domain.model.multitenancy.TenantSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TenantJdbcRepositoryAdapterTest {

    @Mock JdbcTemplate jdbc;
    @Mock NamedParameterJdbcTemplate namedJdbc;
    @InjectMocks TenantJdbcRepositoryAdapter adapter;

    private static TenantSettings settings() {
        return TenantSettings.of("t-1", "tenant_acme");
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

    @Test
    @SuppressWarnings("unchecked")
    void findAll_returnsEmptyListWhenNoRows() {
        when(namedJdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        List<Tenant> result = adapter.findAll(null, null, null, 0, 20);

        assertTrue(result.isEmpty());
    }

    @Test
    void countAll_returnsZeroWhenNull() {
        when(namedJdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(null);

        assertEquals(0L, adapter.countAll(null, null, null));
    }

    @Test
    void countAll_returnsCount() {
        when(namedJdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(4L);

        assertEquals(4L, adapter.countAll("acme", 1, "ACTIVE"));
    }

    @Test
    void countTotal_returnsCount() {
        when(namedJdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(10L);

        assertEquals(10L, adapter.countTotal());
    }

    @Test
    void countActive_returnsCount() {
        when(namedJdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(7L);

        assertEquals(7L, adapter.countActive());
    }

    @Test
    void countCreatedSince_returnsCount() {
        when(namedJdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(3L);

        assertEquals(3L, adapter.countCreatedSince(LocalDateTime.now().minusDays(30)));
    }

    @Test
    void countTotalBefore_returnsCount() {
        when(namedJdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(6L);

        assertEquals(6L, adapter.countTotalBefore(LocalDateTime.now().minusDays(30)));
    }

    @Test
    void countExistedBefore_returnsCount() {
        when(namedJdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(8L);

        assertEquals(8L, adapter.countExistedBefore(LocalDateTime.now().minusDays(30)));
    }

    @Test
    void countDeletedBetween_returnsCountWithOpenUpperBound() {
        when(namedJdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(2L);

        assertEquals(2L, adapter.countDeletedBetween(LocalDateTime.now().minusDays(30), null));
    }

    @Test
    void countDeletedBetween_returnsZeroWhenNull() {
        when(namedJdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(null);

        assertEquals(0L, adapter.countDeletedBetween(LocalDateTime.now().minusDays(60), LocalDateTime.now().minusDays(30)));
    }
}
