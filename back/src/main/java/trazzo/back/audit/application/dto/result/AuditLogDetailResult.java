package trazzo.back.audit.application.dto.result;

import java.time.LocalDateTime;
import java.util.Map;

public record AuditLogDetailResult(
    String id,
    String entidad,
    String entidadId,
    String accion,
    String userId,
    String endpoint,
    String ipAddress,
    String userAgent,
    Map<String, Object> oldValue,
    Map<String, Object> newValue,
    LocalDateTime createdAt
) {}
