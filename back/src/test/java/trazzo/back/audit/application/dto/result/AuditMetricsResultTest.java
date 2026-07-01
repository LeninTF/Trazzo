package trazzo.back.audit.application.dto.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuditMetricsResultTest {

    @Test
    void shouldCreate() {
        var result = new AuditMetricsResult(1000, 5, 42, 3.5, 75.2);
        assertEquals(1000, result.totalEventos());
        assertEquals(5, result.errores());
        assertEquals(42, result.sesionesActivas());
        assertEquals(3.5, result.crecimiento());
        assertEquals(75.2, result.porcentajeSesiones());
    }

    @Test
    void shouldHandleZeroValues() {
        var result = new AuditMetricsResult(0, 0, 0, 0.0, 0.0);
        assertEquals(0, result.totalEventos());
        assertEquals(0, result.errores());
        assertEquals(0, result.sesionesActivas());
        assertEquals(0.0, result.crecimiento());
        assertEquals(0.0, result.porcentajeSesiones());
    }

    @Test
    void shouldHandleNegativeGrowth() {
        var result = new AuditMetricsResult(100, 2, 10, -1.5, 50.0);
        assertEquals(100, result.totalEventos());
        assertEquals(2, result.errores());
        assertEquals(10, result.sesionesActivas());
        assertEquals(-1.5, result.crecimiento());
        assertEquals(50.0, result.porcentajeSesiones());
    }

    @Test
    void shouldTestEquality() {
        var r1 = new AuditMetricsResult(1, 2, 3, 4.0, 5.0);
        var r2 = new AuditMetricsResult(1, 2, 3, 4.0, 5.0);
        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void shouldTestToString() {
        var result = new AuditMetricsResult(99, 1, 5, 1.0, 20.0);
        assertTrue(result.toString().contains("99"));
    }
}
