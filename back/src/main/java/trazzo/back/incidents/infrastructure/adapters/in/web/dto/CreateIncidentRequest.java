package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record CreateIncidentRequest(
        @NotBlank @JsonProperty("incidencia_type_id") String incidenciaTypeId,
        String comment
) {
}
