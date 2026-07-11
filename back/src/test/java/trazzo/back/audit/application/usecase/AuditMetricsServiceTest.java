package trazzo.back.audit.application.usecase;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuditMetricsServiceTest {

    private AuditMetricsService service;

    @BeforeEach
    void setUp() {
        service = new AuditMetricsService();
    }

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
