package trazzo.back.dashboard.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.dashboard.application.port.in.PuntualidadRolUseCase;
import trazzo.back.dashboard.application.port.out.DashboardAttendancePort;
import trazzo.back.dashboard.domain.model.PuntualidadPorRol;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
public class PuntualidadRolService implements PuntualidadRolUseCase {

    private final DashboardAttendancePort attendancePort;

    @Override
    public PuntualidadPorRol getPuntualidadPorRol(LocalDate desde, LocalDate hasta, String periodo) {
        List<PuntualidadPorRol.RolPuntualidad> roles = attendancePort.getPunctualityByRole(desde, hasta)
                .stream()
                .map(r -> {
                    double pct = r.total() > 0
                            ? Math.round((r.punctual() * 10000.0 / r.total())) / 100.0
                            : 0.0;
                    return new PuntualidadPorRol.RolPuntualidad(r.roleName(), pct);
                })
                .toList();

        return new PuntualidadPorRol(periodo, desde.toString(), hasta.toString(), roles);
    }
}
