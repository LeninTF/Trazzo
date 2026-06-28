package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record NotifyIncidentRequest(
        @NotBlank String tipo
) {
}
