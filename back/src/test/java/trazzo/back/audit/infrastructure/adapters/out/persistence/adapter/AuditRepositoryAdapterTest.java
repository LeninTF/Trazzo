package trazzo.back.audit.infrastructure.adapters.out.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import trazzo.back.audit.domain.model.master.Action;
import trazzo.back.audit.domain.model.master.Audit;
import trazzo.back.audit.infrastructure.adapters.out.persistence.repository.AuditJpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditRepositoryAdapterTest {

    @Mock
    private AuditJpaRepository jpaRepository;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AuditRepositoryAdapter adapter;

    private final LocalDateTime now = LocalDateTime.now();

    @Test
    void findById_shouldReturnEmpty() {
        when(jpaRepository.findById(any())).thenReturn(Optional.empty());

        var result = adapter.findById("00000000-0000-0000-0000-000000000001");

        assertThat(result).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_shouldQueryAndMap() {
        var audit = Audit.restore("id-1", "User", "1", Action.CREATE,
                "u1", "/api", "127.0.0.1", "agent", null, null, now);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of(audit));

        var result = adapter.findAll(null, "search", Action.CREATE, "User",
                now, now.plusDays(1), org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEntity()).isEqualTo("User");
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), any(Object[].class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_shouldUseDefaultSort() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        adapter.findAll(null, null, null, null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 10));

        verify(jdbcTemplate).query(contains("ORDER BY a.created_at DESC"), any(RowMapper.class), any(Object[].class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_withSearchAppendsSearchClause() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        adapter.findAll("test", null, null, null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 10));

        verify(jdbcTemplate).query(contains("LOWER(a.entity) LIKE"), any(RowMapper.class), any(Object[].class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_withInvalidTenantUuidSkipsClause() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        adapter.findAll(null, "not-a-uuid", null, null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 10));

        verify(jdbcTemplate).query(contains("1=1"), any(RowMapper.class), any(Object[].class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_withActionAppendsClause() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        adapter.findAll(null, null, Action.DELETE, null, null, null,
                org.springframework.data.domain.PageRequest.of(0, 10));

        verify(jdbcTemplate).query(contains("a.action = ?"), any(RowMapper.class), any(Object[].class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_withEntityAppendsClause() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        adapter.findAll(null, null, null, "User", null, null,
                org.springframework.data.domain.PageRequest.of(0, 10));

        verify(jdbcTemplate).query(contains("a.entity = ?"), any(RowMapper.class), any(Object[].class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findAll_withDateClauses() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(Object[].class)))
                .thenReturn(List.of());

        adapter.findAll(null, null, null, null, now, now.plusDays(1),
                org.springframework.data.domain.PageRequest.of(0, 10));

        verify(jdbcTemplate).query(contains("created_at >= ?"), any(RowMapper.class), any(Object[].class));
        verify(jdbcTemplate).query(contains("created_at <= ?"), any(RowMapper.class), any(Object[].class));
    }

    @Test
    void count_shouldReturnTotalElements() {
        when(jdbcTemplate.queryForObject(anyString(), any(Class.class), any(Object[].class)))
                .thenReturn(5L);

        var result = adapter.count(null, "search", Action.CREATE, "User",
                now, now.plusDays(1));

        assertThat(result).isEqualTo(5L);
    }

    @Test
    void count_shouldReturnZeroWhenNull() {
        when(jdbcTemplate.queryForObject(anyString(), any(Class.class), any(Object[].class)))
                .thenReturn(null);

        var result = adapter.count(null, null, null, null, null, null);

        assertThat(result).isEqualTo(0L);
    }

    @Test
    void deserializeJson_shouldHandleNull() {
        assertThat(AuditRepositoryAdapter.deserializeJson(null)).isNotNull();
    }

    @Test
    void deserializeJson_shouldHandleEmptyString() {
        assertThat(AuditRepositoryAdapter.deserializeJson("")).isNotNull();
    }
}
