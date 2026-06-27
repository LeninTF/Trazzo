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
import trazzo.back.reports.domain.model.closure.MonthlyClosure;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class MonthlyClosureJdbcRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private MonthlyClosureJdbcRepository repository;

    @BeforeEach
    void setUp() {
        repository = new MonthlyClosureJdbcRepository(jdbcTemplate);
    }

    @Test
    void shouldSaveClosure() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosure closure = new MonthlyClosure(id, 6, 2025, 10, "excel", "pdf", "user-1", now);

        when(jdbcTemplate.update(anyString(),
                eq(id), eq(6), eq(2025), eq(10), eq("excel"), eq("pdf"), eq("user-1"), eq(now)))
                .thenReturn(1);

        MonthlyClosure result = repository.save(closure);

        assertEquals(id, result.getId());
        assertEquals(6, result.getMonth());
        verify(jdbcTemplate).update(anyString(),
                eq(id), eq(6), eq(2025), eq(10), eq("excel"), eq("pdf"), eq("user-1"), eq(now));
    }

    @Test
    void shouldFindByIdWhenExists() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosure closure = new MonthlyClosure(id, 6, 2025, 10, "excel", "pdf", "user-1", now);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(id)))
                .thenReturn(List.of(closure));

        Optional<MonthlyClosure> result = repository.findById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
    }

    @Test
    void shouldReturnEmptyWhenFindByIdNotFound() {
        UUID id = UUID.randomUUID();

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(id)))
                .thenReturn(List.of());

        Optional<MonthlyClosure> result = repository.findById(id);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldFindAll() {
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosure c1 = new MonthlyClosure(UUID.randomUUID(), 6, 2025, 10, "e1", "p1", "u1", now);
        MonthlyClosure c2 = new MonthlyClosure(UUID.randomUUID(), 7, 2025, 5, "e2", "p2", "u2", now);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(c1, c2));

        List<MonthlyClosure> results = repository.findAll();

        assertEquals(2, results.size());
    }

    @Test
    void shouldFindByMonthAndYear() {
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosure closure = new MonthlyClosure(UUID.randomUUID(), 6, 2025, 10, "e", "p", "u", now);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(6), eq(2025)))
                .thenReturn(List.of(closure));

        List<MonthlyClosure> results = repository.findByMonthAndYear(6, 2025);

        assertEquals(1, results.size());
        assertEquals(6, results.getFirst().getMonth());
        assertEquals(2025, results.getFirst().getYear());
    }

    @Test
    void shouldFindAndLockByMonthAndYear() {
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosure closure = new MonthlyClosure(UUID.randomUUID(), 6, 2025, 10, "e", "p", "u", now);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(6), eq(2025)))
                .thenReturn(List.of(closure));

        Optional<MonthlyClosure> result = repository.findAndLockByMonthAndYear(6, 2025);

        assertTrue(result.isPresent());
        assertEquals(6, result.get().getMonth());
    }

    @Test
    void shouldReturnEmptyWhenFindAndLockNotFound() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(6), eq(2025)))
                .thenReturn(List.of());

        Optional<MonthlyClosure> result = repository.findAndLockByMonthAndYear(6, 2025);

        assertTrue(result.isEmpty());
    }
}
