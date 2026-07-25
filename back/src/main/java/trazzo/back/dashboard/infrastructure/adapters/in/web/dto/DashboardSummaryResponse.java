package trazzo.back.dashboard.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.dashboard.domain.model.DashboardSummary;

public record DashboardSummaryResponse(
        @JsonProperty("usuarios_activos") long usuariosActivos,
        @JsonProperty("capacidad_plan") double capacidadPlan,
        MetricasResponse metricas,
        @JsonProperty("indice_puntualidad_anual") double indicePuntualidadAnual
) {
    public static DashboardSummaryResponse from(DashboardSummary summary) {
        return new DashboardSummaryResponse(
                summary.usuariosActivos(),
                summary.capacidadPlan(),
                new MetricasResponse(
                        summary.metricas().totalInasistencias(),
                        summary.metricas().totalIncidencias()),
                summary.indicePuntualidadAnual());
    }

    public record MetricasResponse(
            @JsonProperty("total_inasistencias") long totalInasistencias,
            @JsonProperty("total_incidencias") long totalIncidencias
    ) {
    }
}
