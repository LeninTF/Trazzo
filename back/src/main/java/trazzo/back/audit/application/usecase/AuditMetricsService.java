package trazzo.back.audit.application.usecase;

import trazzo.back.audit.application.dto.result.AuditMetricsResult;
import trazzo.back.audit.application.port.in.AuditMetricsUseCase;
import trazzo.back.audit.application.port.out.AuditMetricsPort;

import java.time.Clock;
import java.time.LocalDateTime;

public class AuditMetricsService implements AuditMetricsUseCase {

    private final AuditMetricsPort metricsPort;
    private final Clock clock;

    public AuditMetricsService(AuditMetricsPort metricsPort, Clock clock) {
        this.metricsPort = metricsPort;
        this.clock = clock;
    }

    @Override
    public AuditMetricsResult getMetrics() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime thirtyDaysAgo = now.minusDays(30);
        LocalDateTime sixtyDaysAgo = now.minusDays(60);

        long totalEventos = metricsPort.countAll();
        long recentCount = metricsPort.countSince(thirtyDaysAgo);
        long previousCount = metricsPort.countBetween(sixtyDaysAgo, thirtyDaysAgo);
        long accionesDelete = metricsPort.countByAction("DELETE");
        long sesionesActivas = metricsPort.countActiveSessions();

        double crecimiento = 0.0;
        if (previousCount > 0) {
            crecimiento = ((double) (recentCount - previousCount) / previousCount) * 100;
        }

        double porcentajeSesiones = 0.0;
        if (totalEventos > 0) {
            porcentajeSesiones = ((double) sesionesActivas / totalEventos) * 100;
        }

        return new AuditMetricsResult(
                totalEventos,
                accionesDelete,
                sesionesActivas,
                Math.round(crecimiento * 10.0) / 10.0,
                Math.round(porcentajeSesiones * 10.0) / 10.0
        );
    }
}
