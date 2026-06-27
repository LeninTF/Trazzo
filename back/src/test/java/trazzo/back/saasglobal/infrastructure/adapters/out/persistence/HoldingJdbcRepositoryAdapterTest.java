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
import trazzo.back.saasglobal.domain.model.multitenancy.Holding;
import trazzo.back.saasglobal.domain.model.multitenancy.HoldingType;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class HoldingJdbcRepositoryAdapterTest {

    @Mock JdbcTemplate jdbc;
    @InjectMocks HoldingJdbcRepositoryAdapter adapter;

    private static Holding newHolding() {
        return Holding.create("20123456789", "Empresa SAC", HoldingType.PRIVATE);
    }

    private static Holding savedHolding() {
        var now = LocalDateTime.now();
        return Holding.restore(1, "20123456789", "Empresa SAC", HoldingType.PUBLIC,
                true, now, now, null);
    }

    @Test
    void save_newHolding_returnsHoldingWithId() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any(), any(), any(), any(), any()))
                .thenReturn(42);

        Holding result = adapter.save(newHolding());

        assertEquals(42, result.getId());
        assertEquals("20123456789", result.getTaxId());
    }

    @Test
    void save_existingHolding_returnsSameInstance() {
        var holding = savedHolding();

        Holding result = adapter.save(holding);

        assertSame(holding, result);
        verify(jdbc).update(anyString(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findById_returnsEmptyWhenNotFound() {
        when(jdbc.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());

        Optional<Holding> result = adapter.findById(99);

        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByTaxId_returnsEmptyWhenNotFound() {
        when(jdbc.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());

        Optional<Holding> result = adapter.findByTaxId("00000000000");

        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_returnsEmptyList() {
        when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(List.of());

        List<Holding> result = adapter.findAll();

        assertTrue(result.isEmpty());
    }

    @Test
    void existsByTaxId_returnsTrueWhenCountPositive() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("20123456789"))).thenReturn(1);

        assertTrue(adapter.existsByTaxId("20123456789"));
    }

    @Test
    void existsByTaxId_returnsFalseWhenCountZero() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("00000000000"))).thenReturn(0);

        assertFalse(adapter.existsByTaxId("00000000000"));
    }

    @Test
    void existsByTaxId_returnsFalseWhenNullReturned() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any())).thenReturn(null);

        assertFalse(adapter.existsByTaxId("00000000000"));
    }
}
