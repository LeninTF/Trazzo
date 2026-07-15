package trazzo.back.audit.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
<<<<<<< Updated upstream
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuditMetricsServiceTest {

    private JdbcTemplate jdbcTemplate;
=======

class AuditMetricsServiceTest {

>>>>>>> Stashed changes
    private AuditMetricsService service;

    @BeforeEach
    void setUp() {
<<<<<<< Updated upstream
        jdbcTemplate = mock(JdbcTemplate.class);
        service = new AuditMetricsService(jdbcTemplate);
    }

    @Test
    void getMetricsReturnsZerosForEmptyDb() {
        when(jdbcTemplate.queryForObject(
                org.mockito.ArgumentMatchers.eq("SELECT COUNT(*) FROM audit"),
                org.mockito.ArgumentMatchers.eq(Long.class)))
                .thenReturn(0L);
        when(jdbcTemplate.queryForObject(
                org.mockito.ArgumentMatchers.eq("SELECT COUNT(*) FROM audit WHERE created_at >= ?"),
                org.mockito.ArgumentMatchers.eq(Long.class),
                org.mockito.ArgumentMatchers.any()))
                .thenReturn(0L);
        when(jdbcTemplate.queryForObject(
                org.mockito.ArgumentMatchers.eq("SELECT COUNT(*) FROM audit WHERE created_at >= ? AND created_at < ?"),
                org.mockito.ArgumentMatchers.eq(Long.class),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(0L);
=======
        service = new AuditMetricsService();
    }
>>>>>>> Stashed changes

    @Test
    void getMetricsReturnsHardcodedZeros() {
        var result = service.getMetrics();

        assertEquals(0, result.totalEventos());
        assertEquals(0, result.errores());
        assertEquals(0, result.sesionesActivas());
        assertEquals(0.0, result.crecimiento());
        assertEquals(0.0, result.porcentajeSesiones());
    }
}
