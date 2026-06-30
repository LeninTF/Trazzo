package trazzo.back.audit.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.audit.application.dto.result.AuditLogDetailResult;
import trazzo.back.audit.domain.model.master.Action;
import java.time.LocalDateTime;
import java.util.Map;

public record AuditLogDetailResponse(
    Long id,
    String entity,
    @JsonProperty("entity_id") String entityId,
    Action action,
    @JsonProperty("user_id") String userId,
    String endpoint,
    @JsonProperty("ip_address") String ipAddress,
    @JsonProperty("user_agent") String userAgent,
    @JsonProperty("previous_value") Map<String, Object> previousValue,
    @JsonProperty("new_value") Map<String, Object> newValue,
    @JsonProperty("created_at") LocalDateTime createdAt
) {
    public static AuditLogDetailResponse from(AuditLogDetailResult result) {
        return new AuditLogDetailResponse(
            result.id(), result.entity(), result.entityId(), result.action(),
            result.userId(), result.endpoint(), result.ipAdress(),
            result.userAgent(), result.previousValue(), result.newValue(),
            result.createdAt()
        );
    }
}
