package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import trazzo.back.saasglobal.domain.model.multitenancy.Plan;

@ExtendWith(MockitoExtension.class)
class PlanJdbcRepositoryAdapterTest {

    @Mock JdbcTemplate jdbc;
    @InjectMocks PlanJdbcRepositoryAdapter adapter;

    @Test
    @SuppressWarnings("unchecked")
    void findById_returnsEmptyWhenNotFound() {
        when(jdbc.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());

        Optional<Plan> result = adapter.findById(99);

        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_returnsEmptyList() {
        when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(List.of());

        List<Plan> result = adapter.findAll();

        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAllActive_returnsEmptyList() {
        when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(List.of());

        List<Plan> result = adapter.findAllActive();

        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findById_returnsMappedPlan() throws Exception {
        var now = LocalDateTime.now();
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("id")).thenReturn(1);
        when(rs.getString("name")).thenReturn("Basic");
        when(rs.getBigDecimal("price")).thenReturn(BigDecimal.valueOf(99));
        when(rs.getString("currency")).thenReturn("SOLES");
        when(rs.getString("billing_period")).thenReturn("MONTHLY");
        when(rs.getBoolean("is_active")).thenReturn(true);
        when(rs.getObject("created_at", LocalDateTime.class)).thenReturn(now);
        when(rs.getObject("updated_at", LocalDateTime.class)).thenReturn(now);
        when(rs.getObject("deleted_at", LocalDateTime.class)).thenReturn(null);

        when(jdbc.query(anyString(), any(RowMapper.class), any()))
                .thenAnswer(inv -> {
                    RowMapper<Plan> mapper = inv.getArgument(1);
                    return List.of(mapper.mapRow(rs, 0));
                });

        Optional<Plan> result = adapter.findById(1);

        assertTrue(result.isPresent());
        assertEquals("Basic", result.get().getName());
        assertTrue(result.get().isActive());
    }

    @Test
    void save_insertsNewPlanAndReturnsWithId() {
        Plan newPlan = Plan.create("Basic", BigDecimal.valueOf(99), "SOLES", "MONTHLY");
        when(jdbc.queryForObject(anyString(), eq(Integer.class),
                any(), any(), any(), any(), any(), any(), any())).thenReturn(42);

        Plan saved = adapter.save(newPlan);

        assertEquals(42, saved.getId());
        assertEquals("Basic", saved.getName());
        assertTrue(saved.isActive());
    }

    @Test
    void save_updatesExistingPlanAndReturnsSameInstance() {
        var now = LocalDateTime.now();
        Plan existing = Plan.restore(1, "Basic", BigDecimal.valueOf(99), "SOLES", "MONTHLY",
                true, now, now, null);

        Plan saved = adapter.save(existing);

        assertSame(existing, saved);
        verify(jdbc).update(anyString(), any(), any(), any(), any(), any(), any(), any(), any());
    }
}
