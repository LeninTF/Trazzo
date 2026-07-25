package trazzo.back.dashboard.domain.model;

public record DashboardSummary(
        long usuariosActivos,
        double capacidadPlan,
        MetricasAsistencia metricas,
        double indicePuntualidadAnual
) {
}
