package trazzo.back.audit.infrastructure.adapters.in.web.dto;

import trazzo.back.audit.application.dto.result.AuditLogResult;
import java.time.LocalDateTime;
import java.util.Map;

public record AuditLogResponse(
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
) {
    public static AuditLogResponse from(AuditLogResult result) {
        return new AuditLogResponse(
            result.id(), result.eventId(), result.fecha(),
            result.tenant(), result.tenantId(), result.userName(), result.userEmail(),
            result.accion(), result.tipo(), result.entidad(), result.entidadId(),
            result.ipAddress(), result.userAgent(),
            result.oldValue(), result.newValue()
        );
    }
}
