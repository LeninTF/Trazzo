package trazzo.back.audit.application.dto.result;

import java.time.LocalDateTime;
import java.util.Map;

public record AuditLogResult(
    String id,
    String eventId,
    LocalDateTime fecha,
    String tenant,
    String tenantId,
    String userName,
    String userEmail,
    String accion,
    String tipo,
    String entidad,
    String entidadId,
    String ipAddress,
    String userAgent,
    Map<String, Object> oldValue,
    Map<String, Object> newValue
) {}
