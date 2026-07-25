package trazzo.back.audit.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.audit.application.port.out.AuditRepositoryPort;
import trazzo.back.audit.application.port.out.SessionRepositoryPort;

@ExtendWith(MockitoExtension.class)
class AuditMetricsServiceTest {

    @Mock
    private AuditRepositoryPort auditRepository;

    @Mock
    private SessionRepositoryPort sessionRepository;

    @InjectMocks
    private AuditMetricsService service;

    @Test
    void getMetricsReturnsTotalEventosFromRepository() {
        when(auditRepository.count(isNull(), isNull(), isNull(), any(), any()))
                .thenAnswer(invocation -> {
                    var desde = invocation.getArgument(3, java.time.LocalDateTime.class);
                    return desde == null ? 42L : 5L;
                });
        when(sessionRepository.count(isNull(), any(), isNull())).thenReturn(3L);

        var result = service.getMetrics();

        assertEquals(42, result.totalEventos());
        assertEquals(3, result.sesionesActivas());
        assertTrue(result.porcentajeSesiones() > 0.0);
    }

    @Test
    void getMetricsCalculatesPositiveGrowth() {
        when(auditRepository.count(isNull(), isNull(), isNull(), any(), any()))
                .thenAnswer(invocation -> {
                    var desde = invocation.getArgument(3, java.time.LocalDateTime.class);
                    return desde == null ? 100L : 10L;
                });
        when(sessionRepository.count(isNull(), any(), isNull())).thenReturn(5L);

        var result = service.getMetrics();

        assertTrue(result.crecimiento() >= 0.0);
    }

    @Test
    void getMetricsHandlesZeroYesterdayCount() {
        when(auditRepository.count(isNull(), isNull(), isNull(), any(), any()))
                .thenAnswer(invocation -> {
                    var desde = invocation.getArgument(3, java.time.LocalDateTime.class);
                    return desde == null ? 10L : 0L;
                });
        when(sessionRepository.count(isNull(), any(), isNull())).thenReturn(2L);

        var result = service.getMetrics();

        assertEquals(10, result.totalEventos());
        assertEquals(0.0, result.crecimiento());
    }
}
