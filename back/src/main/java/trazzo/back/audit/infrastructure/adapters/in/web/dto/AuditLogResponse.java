package trazzo.back.audit.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.audit.application.dto.result.AuditLogResult;
import java.time.LocalDateTime;
import java.util.Map;

public record AuditLogResponse(
    Long id,
    @JsonProperty("event_id") String eventId,
    LocalDateTime fecha,
    String tenant,
    @JsonProperty("tenant_id") String tenantId,
    @JsonProperty("user_name") String userName,
    @JsonProperty("user_email") String userEmail,
    String accion,
    String tipo,
    String entidad,
    @JsonProperty("entidad_id") String entidadId,
    @JsonProperty("ip_address") String ipAddress,
    @JsonProperty("user_agent") String userAgent,
    @JsonProperty("old_value") Map<String, Object> oldValue,
    @JsonProperty("new_value") Map<String, Object> newValue
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
