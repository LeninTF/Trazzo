package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import trazzo.back.saasglobal.domain.model.multitenancy.PlanFeature;

@ExtendWith(MockitoExtension.class)
class PlanFeatureJdbcRepositoryAdapterTest {

    @Mock JdbcTemplate jdbc;
    @InjectMocks PlanFeatureJdbcRepositoryAdapter adapter;

    @Test
    void save_insertsAndReturnsWithId() {
        PlanFeature planFeature = PlanFeature.create(1, 10, "INT", "250", LocalDate.now());
        when(jdbc.queryForObject(anyString(), eq(Integer.class),
                any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(7);

        PlanFeature saved = adapter.save(planFeature);

        assertEquals(7, saved.getId());
        assertEquals(1, saved.getPlanId());
        assertEquals(10, saved.getFeatureId());
        assertEquals("250", saved.getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByPlanId_returnsEmptyList() {
        when(jdbc.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());

        List<PlanFeature> result = adapter.findByPlanId(1);

        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByPlanId_returnsMappedFeatures() throws Exception {
        var now = LocalDateTime.now();
        var today = LocalDate.now();
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("id")).thenReturn(1);
        when(rs.getInt("plan_id")).thenReturn(5);
        when(rs.getInt("feature_id")).thenReturn(10);
        when(rs.getString("tipo_dato")).thenReturn("INT");
        when(rs.getString("value")).thenReturn("250");
        when(rs.getObject("date_start", LocalDate.class)).thenReturn(today);
        when(rs.getObject("date_end", LocalDate.class)).thenReturn(null);
        when(rs.getBoolean("is_active")).thenReturn(true);
        when(rs.getObject("created_at", LocalDateTime.class)).thenReturn(now);
        when(rs.getObject("updated_at", LocalDateTime.class)).thenReturn(now);

        when(jdbc.query(anyString(), any(RowMapper.class), any()))
                .thenAnswer(inv -> {
                    RowMapper<PlanFeature> mapper = inv.getArgument(1);
                    return List.of(mapper.mapRow(rs, 0));
                });

        List<PlanFeature> result = adapter.findByPlanId(5);

        assertEquals(1, result.size());
        assertEquals(10, result.get(0).getFeatureId());
        assertEquals("250", result.get(0).getValue());
    }

    @Test
    void deleteByPlanId_executesDelete() {
        adapter.deleteByPlanId(5);

        verify(jdbc).update(anyString(), eq(5));
    }
}
