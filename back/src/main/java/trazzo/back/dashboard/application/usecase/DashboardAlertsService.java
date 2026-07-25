package trazzo.back.dashboard.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.dashboard.application.port.in.DashboardAlertsUseCase;
import trazzo.back.dashboard.application.port.out.DashboardAttendancePort;
import trazzo.back.dashboard.domain.model.AlertaDashboard;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class DashboardAlertsService implements DashboardAlertsUseCase {

    private final DashboardAttendancePort attendancePort;

    @Override
    public AlertaDashboard getAlertas() {
        LocalDate hoy = LocalDate.now();
        List<AlertaDashboard.Alerta> alertas = new ArrayList<>();

        long tardanzasHoy = attendancePort.countByStateAndDateRange("TARDANZA", hoy, hoy);
        if (tardanzasHoy > 0) {
            alertas.add(new AlertaDashboard.Alerta(
                    "bi-clock-fill",
                    "Tardanzas hoy",
                    tardanzasHoy + " empleado" + (tardanzasHoy > 1 ? "s" : "") + " registraron ingreso tarde hoy",
                    LocalDateTime.now(),
                    "danger"
            ));
        }

        List<Long> ausentes = attendancePort.getUserIdsWithoutAttendance(hoy);
        if (!ausentes.isEmpty()) {
            alertas.add(new AlertaDashboard.Alerta(
                    "bi-person-dash-fill",
                    "Personal sin registro",
                    ausentes.size() + " empleado" + (ausentes.size() > 1 ? "s" : "") + " no han marcado ingreso hoy",
                    LocalDateTime.now(),
                    "warning"
            ));
        }

        return new AlertaDashboard(alertas);
    }
}
