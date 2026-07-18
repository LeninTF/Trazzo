package trazzo.back.saasglobal.application.dto.result;

public record TenantMetricsResult(
        long total,
        double crecimientoPct,
        long activos,
        double porcentajeActivos,
        long nuevos30d,
        int nuevosMeta,
        double tasaChurnPct,
        double variacionChurnPct
) {}
