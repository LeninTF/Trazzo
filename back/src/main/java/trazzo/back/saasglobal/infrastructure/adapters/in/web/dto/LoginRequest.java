package trazzo.back.saasglobal.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Email(message = "Email inválido")
        @NotBlank(message = "El email es requerido")
        String email,

        @NotBlank(message = "La contraseña es requerida")
        String password
) {}
