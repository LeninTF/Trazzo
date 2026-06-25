package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import trazzo.back.incidents.domain.model.IncidentState;

public record IncidentStateChangeRequest(
        @NotNull IncidentState state,
        @JsonProperty("days_granted") Integer daysGranted,
        @JsonProperty("motivo_rechazo") String motivoRechazo
) {
}
