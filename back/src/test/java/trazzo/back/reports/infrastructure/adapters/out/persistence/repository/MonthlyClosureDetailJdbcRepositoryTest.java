package trazzo.back.reports.infrastructure.adapters.out.persistence.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import trazzo.back.reports.domain.model.closure.MonthlyClosureDetail;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class MonthlyClosureDetailJdbcRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private MonthlyClosureDetailJdbcRepository repository;

    @BeforeEach
    void setUp() {
        repository = new MonthlyClosureDetailJdbcRepository(jdbcTemplate);
    }

    @Test
    void shouldSaveDetail() {
        UUID id = UUID.randomUUID();
        UUID closureId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosureDetail detail = new MonthlyClosureDetail(
                id, closureId, "user-1", "Juan Perez", "12345678",
                "TI", "Developer", 160.0, 10.0, 1, 5.0, now);

        when(jdbcTemplate.update(anyString(),
                eq(id), eq(closureId), eq("user-1"), eq("Juan Perez"),
                eq("12345678"), eq("TI"), eq("Developer"),
                eq(160.0), eq(10.0), eq(1), eq(5.0), eq(now)))
                .thenReturn(1);

        MonthlyClosureDetail result = repository.save(detail);

        assertEquals(id, result.getId());
        verify(jdbcTemplate).update(anyString(),
                eq(id), eq(closureId), eq("user-1"), eq("Juan Perez"),
                eq("12345678"), eq("TI"), eq("Developer"),
                eq(160.0), eq(10.0), eq(1), eq(5.0), eq(now));
    }

    @Test
    void shouldSaveAllDetails() {
        UUID closureId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosureDetail d1 = new MonthlyClosureDetail(
                UUID.randomUUID(), closureId, "u1", "Juan", "111", "TI", "Dev",
                160.0, 10.0, 1, 5.0, now);
        MonthlyClosureDetail d2 = new MonthlyClosureDetail(
                UUID.randomUUID(), closureId, "u2", "Ana", "222", "HR", "Mgr",
                80.0, 15.0, 2, 0.0, now);

        List<MonthlyClosureDetail> results = repository.saveAll(List.of(d1, d2));

        assertEquals(2, results.size());
        verify(jdbcTemplate, times(2)).update(anyString(), any(Object[].class));
    }

    @Test
    void shouldFindByIdWhenExists() {
        UUID id = UUID.randomUUID();
        UUID closureId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosureDetail detail = new MonthlyClosureDetail(
                id, closureId, "user-1", "Juan", "123", "TI", "Dev",
                160.0, 10.0, 1, 5.0, now);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(id)))
                .thenReturn(List.of(detail));

        Optional<MonthlyClosureDetail> result = repository.findById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
    }

    @Test
    void shouldReturnEmptyWhenFindByIdNotFound() {
        UUID id = UUID.randomUUID();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(id)))
                .thenReturn(List.of());

        Optional<MonthlyClosureDetail> result = repository.findById(id);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindByMonthlyClosureId() {
        UUID closureId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosureDetail detail = new MonthlyClosureDetail(
                UUID.randomUUID(), closureId, "u1", "Juan", "123", "TI", "Dev",
                160.0, 10.0, 1, 5.0, now);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(closureId)))
                .thenReturn(List.of(detail));

        List<MonthlyClosureDetail> results = repository.findByMonthlyClosureId(closureId);

        assertEquals(1, results.size());
        assertEquals(closureId, results.getFirst().getMonthClosureId());
    }
}
