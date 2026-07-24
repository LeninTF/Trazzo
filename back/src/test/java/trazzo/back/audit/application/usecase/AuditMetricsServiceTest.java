package trazzo.back.audit.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.audit.application.port.out.AuditMetricsPort;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

class AuditMetricsServiceTest {

    private AuditMetricsPort metricsPort;
    private AuditMetricsService service;

    @BeforeEach
    void setUp() {
        metricsPort = mock(AuditMetricsPort.class);
        Clock fixedClock = Clock.fixed(Instant.parse("2026-01-15T00:00:00Z"), ZoneId.of("America/Mexico_City"));
        service = new AuditMetricsService(metricsPort, fixedClock);
    }

    @Test
    void getMetricsReturnsZerosForEmptyDb() {
        when(metricsPort.countAll()).thenReturn(0L);
        when(metricsPort.countSince(org.mockito.ArgumentMatchers.any())).thenReturn(0L);
        when(metricsPort.countBetween(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(0L);
        when(metricsPort.countByAction("DELETE")).thenReturn(0L);
        when(metricsPort.countActiveSessions()).thenReturn(0L);

        var result = service.getMetrics();

        assertEquals(0, result.totalEventos());
        assertEquals(0, result.errores());
        assertEquals(0, result.sesionesActivas());
        assertEquals(0.0, result.crecimiento());
        assertEquals(0.0, result.porcentajeSesiones());
    }

    @Test
    void getMetricsCalculatesGrowth() {
        when(metricsPort.countAll()).thenReturn(150L);
        when(metricsPort.countSince(org.mockito.ArgumentMatchers.any())).thenReturn(100L);
        when(metricsPort.countBetween(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(50L);
        when(metricsPort.countByAction("DELETE")).thenReturn(5L);
        when(metricsPort.countActiveSessions()).thenReturn(10L);

        var result = service.getMetrics();

        assertEquals(150, result.totalEventos());
        assertEquals(5, result.errores());
        assertEquals(10, result.sesionesActivas());
        assertEquals(100.0, result.crecimiento(), 0.1);
        assertEquals(6.7, result.porcentajeSesiones(), 0.1);
    }

    @Test
    void getMetricsHandlesZeroCounts() {
        when(metricsPort.countAll()).thenReturn(0L);
        when(metricsPort.countSince(org.mockito.ArgumentMatchers.any())).thenReturn(0L);
        when(metricsPort.countBetween(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(0L);
        when(metricsPort.countByAction("DELETE")).thenReturn(0L);
        when(metricsPort.countActiveSessions()).thenReturn(0L);

        var result = service.getMetrics();

        assertEquals(0, result.totalEventos());
        assertEquals(0, result.errores());
        assertEquals(0, result.sesionesActivas());
        assertEquals(0.0, result.crecimiento());
        assertEquals(0.0, result.porcentajeSesiones());
    }

    @Test
    void getMetricsNoSessions() {
        when(metricsPort.countAll()).thenReturn(10L);
        when(metricsPort.countSince(org.mockito.ArgumentMatchers.any())).thenReturn(3L);
        when(metricsPort.countBetween(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(2L);
        when(metricsPort.countByAction("DELETE")).thenReturn(1L);
        when(metricsPort.countActiveSessions()).thenReturn(0L);

        var result = service.getMetrics();

        assertEquals(10, result.totalEventos());
        assertEquals(1, result.errores());
        assertEquals(0, result.sesionesActivas());
        assertEquals(50.0, result.crecimiento(), 0.1);
        assertEquals(0.0, result.porcentajeSesiones());
    }
}
