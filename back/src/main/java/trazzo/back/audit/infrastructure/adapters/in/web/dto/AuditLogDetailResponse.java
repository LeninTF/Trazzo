package trazzo.back.audit.infrastructure.adapters.in.web.dto;

import trazzo.back.audit.application.dto.result.AuditLogDetailResult;
import trazzo.back.audit.domain.model.master.Action;
import java.time.LocalDateTime;
import java.util.Map;

public record AuditLogDetailResponse(
    String id,
    String entity,
    String entityId,
    Action action,
    String userId,
    String endpoint,
    String ipAddress,
    String userAgent,
    Map<String, Object> oldValue,
    Map<String, Object> newValue,
    LocalDateTime createdAt
) {
    public static AuditLogDetailResponse from(AuditLogDetailResult result) {
        return new AuditLogDetailResponse(
            result.id(), result.entity(), result.entityId(), result.action(),
            result.userId(), result.endpoint(), result.ipAddress(),
            result.userAgent(), result.previousValue(), result.newValue(),
            result.createdAt()
        );
    }
}
