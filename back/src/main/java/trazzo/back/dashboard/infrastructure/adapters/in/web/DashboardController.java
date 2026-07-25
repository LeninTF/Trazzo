package trazzo.back.dashboard.infrastructure.adapters.in.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import trazzo.back.dashboard.application.port.in.DashboardAlertsUseCase;
import trazzo.back.dashboard.application.port.in.DashboardSummaryUseCase;
import trazzo.back.dashboard.application.port.in.PuntualidadRolUseCase;
import trazzo.back.dashboard.infrastructure.adapters.in.web.dto.AlertasResponse;
import trazzo.back.dashboard.infrastructure.adapters.in.web.dto.DashboardSummaryResponse;
import trazzo.back.dashboard.infrastructure.adapters.in.web.dto.PuntualidadRolResponse;

import java.time.LocalDate;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardSummaryUseCase summaryUseCase;
    private final PuntualidadRolUseCase puntualidadRolUseCase;
    private final DashboardAlertsUseCase alertsUseCase;

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('dashboard.ver')")
    public ResponseEntity<DashboardSummaryResponse> getSummary(
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta
    ) {
        LocalDate[] range = resolveRange(periodo, desde, hasta);
        var result = summaryUseCase.getSummary(range[0], range[1]);
        return ResponseEntity.ok(DashboardSummaryResponse.from(result));
    }

    @GetMapping("/puntualidad-por-rol")
    @PreAuthorize("hasAuthority('dashboard.ver')")
    public ResponseEntity<PuntualidadRolResponse> getPuntualidadPorRol(
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta
    ) {
        LocalDate[] range = resolveRange(periodo, desde, hasta);
        String resolvedPeriodo = periodo != null ? periodo : "mes";
        var result = puntualidadRolUseCase.getPuntualidadPorRol(range[0], range[1], resolvedPeriodo);
        return ResponseEntity.ok(PuntualidadRolResponse.from(result));
    }

    @GetMapping("/alertas")
    @PreAuthorize("hasAuthority('dashboard.ver')")
    public ResponseEntity<AlertasResponse> getAlertas() {
        var result = alertsUseCase.getAlertas();
        return ResponseEntity.ok(AlertasResponse.from(result));
    }

    private LocalDate[] resolveRange(String periodo, LocalDate desde, LocalDate hasta) {
        LocalDate today = LocalDate.now();
        if (desde != null && hasta != null) {
            return new LocalDate[]{desde, hasta};
        }
        if (periodo == null) periodo = "mes";
        return switch (periodo) {
            case "dia" -> new LocalDate[]{today, today};
            case "semana" -> new LocalDate[]{today.minusWeeks(1), today};
            case "anio" -> new LocalDate[]{today.withDayOfYear(1), today};
            default -> new LocalDate[]{today.withDayOfMonth(1), today};
        };
    }
}
