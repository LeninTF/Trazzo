package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CompleteEnrollRequest(
        @NotBlank @JsonProperty("enroll_token") String enrollToken,
        @NotBlank @JsonProperty("template_cifrado") String templateCifrado,
        @NotBlank @JsonProperty("llave_cifrado") String llaveCifrado,
        @NotNull @JsonProperty("finger_index") Integer fingerIndex,
        @NotBlank @JsonProperty("device_code") String deviceCode,
        @JsonProperty("capturado_en") LocalDateTime capturadoEn
) {
}
