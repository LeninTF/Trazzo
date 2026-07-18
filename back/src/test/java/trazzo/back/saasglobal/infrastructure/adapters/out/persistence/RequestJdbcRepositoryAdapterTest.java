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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import trazzo.back.saasglobal.domain.model.request.Request;

@ExtendWith(MockitoExtension.class)
class RequestJdbcRepositoryAdapterTest {

    @Mock JdbcTemplate jdbc;
    @Mock NamedParameterJdbcTemplate namedJdbc;
    @InjectMocks RequestJdbcRepositoryAdapter adapter;

    @Test
    void save_insertsNewRequestAndReturnsWithId() {
        Request newRequest = Request.create(Request.Type.TRIAL, "Solicitud de trial - Acme SAC", "Quiero una demo");
        when(jdbc.queryForObject(anyString(), eq(Integer.class), any(), any(), any(), any(), any(), any()))
                .thenReturn(42);

        Request saved = adapter.save(newRequest);

        assertEquals(42, saved.getId());
        assertEquals(Request.Status.PENDING, saved.getStatus());
    }

    @Test
    void save_updatesExistingRequest() {
        var now = LocalDateTime.now();
        Request existing = Request.restore(1, Request.Type.TRIAL, "title", "message", Request.Status.APPROVED, now, now);

        Request saved = adapter.save(existing);

        assertSame(existing, saved);
        verify(jdbc).update(anyString(), eq("APPROVED"), any(), eq(1));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findById_returnsEmptyWhenNotFound() {
        when(jdbc.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());

        Optional<Request> result = adapter.findById(99);

        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findById_returnsMappedRequest() throws Exception {
        var now = LocalDateTime.now();
        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt("id")).thenReturn(1);
        when(rs.getString("type")).thenReturn("TRIAL");
        when(rs.getString("title")).thenReturn("title");
        when(rs.getString("message")).thenReturn("message");
        when(rs.getString("status")).thenReturn("PENDING");
        when(rs.getObject("created_at", LocalDateTime.class)).thenReturn(now);
        when(rs.getObject("updated_at", LocalDateTime.class)).thenReturn(now);

        when(jdbc.query(anyString(), any(RowMapper.class), any()))
                .thenAnswer(inv -> {
                    RowMapper<Request> mapper = inv.getArgument(1);
                    return List.of(mapper.mapRow(rs, 0));
                });

        Optional<Request> result = adapter.findById(1);

        assertTrue(result.isPresent());
        assertEquals(Request.Type.TRIAL, result.get().getType());
        assertEquals(Request.Status.PENDING, result.get().getStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByFilters_returnsEmptyListWhenNoMatches() {
        when(namedJdbc.query(anyString(), any(org.springframework.jdbc.core.namedparam.MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        List<Request> result = adapter.findByFilters(Request.Status.PENDING, Request.Type.TRIAL, "acme", 0, 20);

        assertTrue(result.isEmpty());
    }

    @Test
    void countByFilters_returnsZeroWhenNull() {
        when(namedJdbc.queryForObject(anyString(), any(org.springframework.jdbc.core.namedparam.MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(null);

        assertEquals(0L, adapter.countByFilters(null, null, null));
    }

    @Test
    void countByFilters_returnsCount() {
        when(namedJdbc.queryForObject(anyString(), any(org.springframework.jdbc.core.namedparam.MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(5L);

        assertEquals(5L, adapter.countByFilters(Request.Status.PENDING, null, null));
    }
}
