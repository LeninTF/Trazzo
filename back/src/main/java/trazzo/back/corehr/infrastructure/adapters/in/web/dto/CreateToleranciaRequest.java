package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import trazzo.back.corehr.domain.model.ToleranciaType;

public record CreateToleranciaRequest(
        @NotNull ToleranciaType type,
        @NotNull @Min(0) @Max(60) Integer minutes,
        String name,
        String description
) {
}
