package trazzo.back.corehr.application.dto.result;

import java.time.LocalDateTime;
import java.util.UUID;

/** El token se entrega una sola vez: el backend solo guarda su SHA-256. */
public record IssuedAgentToken(String token, UUID tenantId, String deviceCode, LocalDateTime createdAt) {
}
