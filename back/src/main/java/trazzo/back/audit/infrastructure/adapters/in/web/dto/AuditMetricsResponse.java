package trazzo.back.audit.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.audit.application.dto.result.AuditMetricsResult;

public record AuditMetricsResponse(
    @JsonProperty("total_eventos") long totalEventos,
    @JsonProperty("acciones_delete") long accionesDelete,
    @JsonProperty("sesiones_activas") long sesionesActivas,
    @JsonProperty("crecimiento") double crecimiento,
    @JsonProperty("porcentaje_sesiones") double porcentajeSesiones
) {
    public static AuditMetricsResponse from(AuditMetricsResult result) {
        return new AuditMetricsResponse(
            result.totalEventos(), result.accionesDelete(), result.sesionesActivas(),
            result.crecimiento(), result.porcentajeSesiones()
        );
    }
}
