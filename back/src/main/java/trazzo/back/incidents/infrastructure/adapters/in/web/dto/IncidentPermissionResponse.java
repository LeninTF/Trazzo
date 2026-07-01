package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.incidents.application.dto.result.IncidentPermissionResult;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record IncidentPermissionResponse(
        String id,
        @JsonProperty("incidencia_id") String incidenciaId,
        @JsonProperty("start_date") LocalDate startDate,
        @JsonProperty("end_date") LocalDate endDate,
        @JsonProperty("days_granted") int daysGranted,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt
) {
    public static IncidentPermissionResponse from(IncidentPermissionResult result) {
        return new IncidentPermissionResponse(result.id(), result.incidenciaId(), result.startDate(),
                result.endDate(), result.daysGranted(), result.createdAt(), result.updatedAt());
    }
}
