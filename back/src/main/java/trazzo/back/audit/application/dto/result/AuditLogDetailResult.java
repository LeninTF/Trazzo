package trazzo.back.audit.application.dto.result;

import trazzo.back.audit.domain.model.master.Action;
import java.time.LocalDateTime;
import java.util.Map;

public record AuditLogDetailResult(
    String id,
    String entity,
    String entityId,
    Action action,
    String userId,
    String endpoint,
    String ipAddress,
    String userAgent,
    Map<String, Object> previousValue,
    Map<String, Object> newValue,
    LocalDateTime createdAt
) {}
