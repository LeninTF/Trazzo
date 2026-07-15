package trazzo.back.audit.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class AuditMetricsServiceTest {

    private JdbcTemplate jdbcTemplate;
    private AuditMetricsService service;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new AuditMetricsService(jdbcTemplate);
    }

    @Test
    void getMetricsReturnsZerosForEmptyDb() {
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM audit"),
                eq(Long.class)))
                .thenReturn(0L);
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM audit WHERE created_at >= ?"),
                eq(Long.class),
                any()))
                .thenReturn(0L);
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM audit WHERE created_at >= ? AND created_at < ?"),
                eq(Long.class),
                any(), any()))
                .thenReturn(0L);

        var result = service.getMetrics();

        assertEquals(0, result.totalEventos());
        assertEquals(0, result.errores());
        assertEquals(0, result.sesionesActivas());
        assertEquals(0.0, result.crecimiento());
        assertEquals(0.0, result.porcentajeSesiones());
    }
}
