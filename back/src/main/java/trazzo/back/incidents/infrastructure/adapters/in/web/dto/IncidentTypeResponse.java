package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.incidents.application.dto.result.IncidentTypeResult;

import java.time.LocalDateTime;

public record IncidentTypeResponse(
        String id,
        String nombre,
        String descripcion,
        boolean activo,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt
) {
    public static IncidentTypeResponse from(IncidentTypeResult result) {
        return new IncidentTypeResponse(result.id(), result.nombre(), result.descripcion(),
                result.activo(), result.createdAt(), result.updatedAt());
    }
}
