package trazzo.back.shared.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/** Agente biométrico autenticado con el token de su dispositivo (autoridad ROLE_AGENT). */
public record DeviceAgentPrincipal(UUID tenantId, String schemaName, Long deviceId, String deviceCode) {

    public static final String AUTHORITY = "ROLE_AGENT";

    /** Si quien llama es un agente, el device_code de la petición tiene que ser el de su token (403 si no). */
    public static void requireOwnDevice(String deviceCode) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof DeviceAgentPrincipal agent
                && !agent.deviceCode().equals(deviceCode)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El token del agente pertenece a otro dispositivo");
        }
    }
}
