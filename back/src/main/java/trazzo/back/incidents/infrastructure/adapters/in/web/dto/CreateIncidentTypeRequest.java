package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateIncidentTypeRequest(
        @NotBlank @Size(max = 100) String nombre,
        String descripcion
) {
}
