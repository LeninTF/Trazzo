package trazzo.back.audit.application.dto.result;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AuditMetricsResult(
    @JsonProperty("total_eventos") long totalEventos,
    @JsonProperty("errores") long errores,
    @JsonProperty("sesiones_activas") long sesionesActivas,
    @JsonProperty("crecimiento") double crecimiento,
    @JsonProperty("porcentaje_sesiones") double porcentajeSesiones
) {}
