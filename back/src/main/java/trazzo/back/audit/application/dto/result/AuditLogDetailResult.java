package trazzo.back.audit.application.dto.result;

import trazzo.back.audit.domain.model.master.Action;
import java.time.LocalDateTime;
import java.util.Map;

public record AuditLogDetailResult(
    String id,
    String entidad,
    String entidadId,
    Action accion,
    String userId,
    String endpoint,
    String ipAddress,
    String userAgent,
    Map<String, Object> oldValue,
    Map<String, Object> newValue,
    LocalDateTime createdAt
) {}
