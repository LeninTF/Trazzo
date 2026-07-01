package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.ResultSet;
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
import trazzo.back.saasglobal.domain.model.multitenancy.Feature;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FeatureJdbcRepositoryAdapterTest {

    @Mock JdbcTemplate jdbc;
    @InjectMocks FeatureJdbcRepositoryAdapter adapter;

    private static Feature existingFeature() {
        var now = LocalDateTime.now();
        return Feature.restore(1, "Biometric Access", "Fingerprint auth", now, now);
    }

    @Test
    void save_newFeature_returnsFeatureWithId() {
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any(), any(), any()))
                .thenReturn(7);

        Feature result = adapter.save(Feature.create("Biometric Access", "Fingerprint auth"));

        assertEquals(7, result.getId());
        assertEquals("Biometric Access", result.getName());
    }

    @Test
    void save_existingFeature_returnsSameInstance() {
        var feature = existingFeature();

        Feature result = adapter.save(feature);

        assertSame(feature, result);
        verify(jdbc).update(anyString(), any(), any(), any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findById_returnsEmptyWhenNotFound() {
        when(jdbc.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());

        Optional<Feature> result = adapter.findById(99);

        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_returnsEmptyList() {
        when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(List.of());

        List<Feature> result = adapter.findAll();

        assertTrue(result.isEmpty());
    }

    @Test
    void deleteById_invokesJdbc() {
        adapter.deleteById(5);

        verify(jdbc).update(anyString(), eq(5));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findById_returnsMappedFeature() throws Exception {
        var now = LocalDateTime.now();
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("id")).thenReturn(1);
        when(rs.getString("name")).thenReturn("Biometric Access");
        when(rs.getString("description")).thenReturn("Fingerprint auth");
        when(rs.getObject("created_at", LocalDateTime.class)).thenReturn(now);
        when(rs.getObject("updated_at", LocalDateTime.class)).thenReturn(now);

        when(jdbc.query(anyString(), any(RowMapper.class), any()))
                .thenAnswer(inv -> {
                    RowMapper<Feature> mapper = inv.getArgument(1);
                    return List.of(mapper.mapRow(rs, 0));
                });

        Optional<Feature> result = adapter.findById(1);

        assertTrue(result.isPresent());
        assertEquals("Biometric Access", result.get().getName());
        assertEquals("Fingerprint auth", result.get().getDescription());
    }
}
