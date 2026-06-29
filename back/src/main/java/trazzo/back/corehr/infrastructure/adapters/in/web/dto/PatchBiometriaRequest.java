package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.NotNull;

public record PatchBiometriaRequest(
        @NotNull Boolean activo
) {
}
