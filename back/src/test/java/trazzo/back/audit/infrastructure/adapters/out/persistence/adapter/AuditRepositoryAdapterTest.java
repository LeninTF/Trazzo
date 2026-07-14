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
import java.util.UUID;

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
    void count_shouldReturnTotalElements() {
        when(jdbcTemplate.queryForObject(anyString(), any(Class.class), any(Object[].class)))
                .thenReturn(5L);

        var result = adapter.count(null, "search", Action.CREATE, "User",
                now, now.plusDays(1));

        assertThat(result).isEqualTo(5L);
    }
}
