package trazzo.back.audit.application.dto.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuditMetricsResult(
    @JsonProperty("total_eventos") long totalEventos,
    long errores,
    @JsonProperty("sesiones_activas") long sesionesActivas,
    double crecimiento,
    @JsonProperty("porcentaje_sesiones") double porcentajeSesiones
) {}
