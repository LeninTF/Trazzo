package trazzo.back.audit.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

class AuditMetricsServiceTest {

    private JdbcTemplate jdbcTemplate;
    private AuditMetricsService service;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        Clock fixedClock = Clock.fixed(Instant.parse("2026-01-15T00:00:00Z"), ZoneId.of("America/Mexico_City"));
        service = new AuditMetricsService(jdbcTemplate, fixedClock);
    }

    @Test
    void getMetricsReturnsZerosForEmptyDb() {
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit",
                Long.class))
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
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit WHERE action = 'DELETE'",
                Long.class))
                .thenReturn(0L);
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sesion WHERE state = 'ACTIVE'",
                Long.class))
                .thenReturn(0L);

        var result = service.getMetrics();

        assertEquals(0, result.totalEventos());
        assertEquals(0, result.errores());
        assertEquals(0, result.sesionesActivas());
        assertEquals(0.0, result.crecimiento());
        assertEquals(0.0, result.porcentajeSesiones());
    }

    @Test
    void getMetricsCalculatesGrowth() {
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit",
                Long.class))
                .thenReturn(150L);
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM audit WHERE created_at >= ?"),
                eq(Long.class),
                any()))
                .thenReturn(100L);
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM audit WHERE created_at >= ? AND created_at < ?"),
                eq(Long.class),
                any(), any()))
                .thenReturn(50L);
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit WHERE action = 'DELETE'",
                Long.class))
                .thenReturn(5L);
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sesion WHERE state = 'ACTIVE'",
                Long.class))
                .thenReturn(10L);

        var result = service.getMetrics();

        assertEquals(150, result.totalEventos());
        assertEquals(5, result.errores());
        assertEquals(10, result.sesionesActivas());
        assertEquals(100.0, result.crecimiento(), 0.1);
        assertEquals(6.7, result.porcentajeSesiones(), 0.1);
    }

    @Test
    void getMetricsHandlesNullCounts() {
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit",
                Long.class))
                .thenReturn(null);
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM audit WHERE created_at >= ?"),
                eq(Long.class),
                any()))
                .thenReturn(null);
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM audit WHERE created_at >= ? AND created_at < ?"),
                eq(Long.class),
                any(), any()))
                .thenReturn(null);
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit WHERE action = 'DELETE'",
                Long.class))
                .thenReturn(null);
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sesion WHERE state = 'ACTIVE'",
                Long.class))
                .thenReturn(null);

        var result = service.getMetrics();

        assertEquals(0, result.totalEventos());
        assertEquals(0, result.errores());
        assertEquals(0, result.sesionesActivas());
        assertEquals(0.0, result.crecimiento());
        assertEquals(0.0, result.porcentajeSesiones());
    }

    @Test
    void getMetricsReturnsZeroesWhenSesionTableMissing() {
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit",
                Long.class))
                .thenReturn(10L);
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM audit WHERE created_at >= ?"),
                eq(Long.class),
                any()))
                .thenReturn(3L);
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM audit WHERE created_at >= ? AND created_at < ?"),
                eq(Long.class),
                any(), any()))
                .thenReturn(2L);
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit WHERE action = 'DELETE'",
                Long.class))
                .thenReturn(1L);
        when(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sesion WHERE state = 'ACTIVE'",
                Long.class))
                .thenThrow(new DataAccessResourceFailureException("Table not found"));

        var result = service.getMetrics();

        assertEquals(10, result.totalEventos());
        assertEquals(1, result.errores());
        assertEquals(0, result.sesionesActivas());
        assertEquals(50.0, result.crecimiento(), 0.1);
        assertEquals(0.0, result.porcentajeSesiones());
    }
}
