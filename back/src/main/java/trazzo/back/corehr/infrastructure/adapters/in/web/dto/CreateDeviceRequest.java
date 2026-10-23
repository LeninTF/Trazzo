package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateDeviceRequest(
        // Viaja en URLs del agente y en el comando de Configure-Agent.ps1 que genera el panel.
        @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]{1,100}", message = "solo letras, digitos, punto, guion y guion bajo (max. 100)")
        String code,
        String name,
        @NotNull @JsonProperty("branch_id") Long branchId,
        String ip,
        Integer puerto,
        String ubicacion
) {
}
