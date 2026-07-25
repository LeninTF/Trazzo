package trazzo.back.dashboard.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.dashboard.application.port.in.DashboardSummaryUseCase;
import trazzo.back.dashboard.application.port.out.DashboardAttendancePort;
import trazzo.back.dashboard.application.port.out.DashboardIncidentPort;
import trazzo.back.dashboard.domain.model.DashboardSummary;
import trazzo.back.dashboard.domain.model.MetricasAsistencia;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RequiredArgsConstructor
public class DashboardSummaryService implements DashboardSummaryUseCase {

    private final DashboardAttendancePort attendancePort;
    private final DashboardIncidentPort incidentPort;

    @Override
    public DashboardSummary getSummary(LocalDate desde, LocalDate hasta) {
        long usuariosActivos = attendancePort.countActiveUsers();

        long inasistencias = attendancePort.countByStateAndDateRange("FALTA", desde, hasta);

        LocalDateTime desdeDateTime = desde.atStartOfDay();
        LocalDateTime hastaDateTime = hasta.plusDays(1).atStartOfDay();
        long incidencias = incidentPort.countByDateRange(desdeDateTime, hastaDateTime);

        int anioActual = LocalDate.now().getYear();
        long puntualesAnio = attendancePort.countPunctualByYear(anioActual);
        long totalAnio = attendancePort.countTotalByYear(anioActual);
        double indicePuntualidad = totalAnio > 0
                ? Math.round((puntualesAnio * 10000.0 / totalAnio)) / 100.0
                : 0.0;

        MetricasAsistencia metricas = new MetricasAsistencia(inasistencias, incidencias);

        return new DashboardSummary(usuariosActivos, 0.0, metricas, indicePuntualidad);
    }
}
