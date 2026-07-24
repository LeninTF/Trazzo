package trazzo.back.saasglobal.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record UsuarioResponse(
        Integer id,
        String nombre,
        @JsonProperty("apellido_paterno") String apellidoPaterno,
        @JsonProperty("apellido_materno") String apellidoMaterno,
        String email,
        String status,
        @JsonProperty("ultimo_acceso") String ultimoAcceso,
        List<RoleProfileResponse> rol,
        @JsonProperty("tenant_permissions") List<String> tenantPermissions
) {}
