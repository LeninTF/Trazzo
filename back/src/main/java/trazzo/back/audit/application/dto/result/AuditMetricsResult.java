package trazzo.back.audit.application.dto.result;

public record AuditMetricsResult(
    long totalEventos,
    long errores,
    long sesionesActivas,
    double crecimiento,
    double porcentajeSesiones
) {}
