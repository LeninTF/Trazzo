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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class MonthlyClosureDetailJdbcRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ResultSet resultSet;

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
                id, closureId, 1L, "Juan Perez", "12345678",
                "TI", "Developer", 160.0, 10, 1, 5.0, now);

        when(jdbcTemplate.update(anyString(),
                eq(id), eq(closureId), eq(1L), eq("Juan Perez"),
                eq("12345678"), eq("TI"), eq("Developer"),
                eq(160.0), eq(10), eq(1), eq(5.0), eq(now)))
                .thenReturn(1);

        MonthlyClosureDetail result = repository.save(detail);

        assertEquals(id, result.getId());
        verify(jdbcTemplate).update(anyString(),
                eq(id), eq(closureId), eq(1L), eq("Juan Perez"),
                eq("12345678"), eq("TI"), eq("Developer"),
                eq(160.0), eq(10), eq(1), eq(5.0), eq(now));
    }

    @Test
    void shouldSaveAllDetails() {
        UUID closureId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosureDetail d1 = new MonthlyClosureDetail(
                UUID.randomUUID(), closureId, 1L, "Juan", "111", "TI", "Dev",
                160.0, 10, 1, 5.0, now);
        MonthlyClosureDetail d2 = new MonthlyClosureDetail(
                UUID.randomUUID(), closureId, 2L, "Ana", "222", "HR", "Mgr",
                80.0, 15, 2, 0.0, now);

        List<MonthlyClosureDetail> results = repository.saveAll(List.of(d1, d2));

        assertEquals(2, results.size());
        verify(jdbcTemplate, times(2)).update(anyString(), any(Object[].class));
    }

    @Test
    void shouldMapRowCorrectly() throws SQLException {
        UUID id = UUID.randomUUID();
        UUID closureId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        when(resultSet.getObject("id", UUID.class)).thenReturn(id);
        when(resultSet.getObject("monthly_closures_id", UUID.class)).thenReturn(closureId);
        when(resultSet.getLong("tenant_user_id")).thenReturn(1L);
        when(resultSet.getString("tenant_user_full_name")).thenReturn("Juan");
        when(resultSet.getString("tenant_user_document")).thenReturn("123");
        when(resultSet.getString("department_name")).thenReturn("TI");
        when(resultSet.getString("role_name")).thenReturn("Dev");
        when(resultSet.getObject("total_worked_hours", Double.class)).thenReturn(160.0);
        when(resultSet.getObject("total_tardiness_minutes", Integer.class)).thenReturn(10);
        when(resultSet.getInt("total_absences")).thenReturn(1);
        when(resultSet.getObject("total_overtime_hours", Double.class)).thenReturn(5.0);
        when(resultSet.getObject("created_at", LocalDateTime.class)).thenReturn(now);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(id)))
                .thenAnswer(invocation -> {
                    RowMapper<MonthlyClosureDetail> mapper = invocation.getArgument(1);
                    return List.of(mapper.mapRow(resultSet, 1));
                });

        Optional<MonthlyClosureDetail> result = repository.findById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
        assertEquals(closureId, result.get().getMonthClosureId());
        assertEquals("Juan", result.get().getTenantUserFullName());
    }

    @Test
    void shouldFindByIdWhenExists() {
        UUID id = UUID.randomUUID();
        UUID closureId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosureDetail detail = new MonthlyClosureDetail(
                id, closureId, 1L, "Juan", "123", "TI", "Dev",
                160.0, 10, 1, 5.0, now);

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
                UUID.randomUUID(), closureId, 1L, "Juan", "123", "TI", "Dev",
                160.0, 10, 1, 5.0, now);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(closureId)))
                .thenReturn(List.of(detail));

        List<MonthlyClosureDetail> results = repository.findByMonthlyClosureId(closureId);

        assertEquals(1, results.size());
        assertEquals(closureId, results.getFirst().getMonthClosureId());
    }
}
