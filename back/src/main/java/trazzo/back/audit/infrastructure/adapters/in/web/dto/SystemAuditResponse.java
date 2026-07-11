package trazzo.back.audit.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.audit.application.dto.result.SystemAuditResult;
import trazzo.back.audit.domain.model.tenant.HttpMethod;
import trazzo.back.audit.domain.model.tenant.SystemActions;
import java.time.LocalDateTime;
import java.util.Map;

public record SystemAuditResponse(
    Long id,
    @JsonProperty("user_tenant_id") String userTenantId,
    @JsonProperty("system_actions") SystemActions systemActions,
    String module,
    String entity,
    @JsonProperty("entity_id") String entityId,
    @JsonProperty("http_method") HttpMethod httpMethod,
    String endpoint,
    String description,
    @JsonProperty("previous_value") Map<String, Object> previousValue,
    @JsonProperty("new_value") Map<String, Object> newValue,
    @JsonProperty("ip_address") String ipAddress,
    String result,
    @JsonProperty("created_at") LocalDateTime createdAt
) {
    public static SystemAuditResponse from(SystemAuditResult result) {
        return new SystemAuditResponse(
            result.id(), result.userTenantId(), result.systemActions(),
            result.module(), result.entity(), result.entityId(),
            result.httpMethod(), result.endpoint(), result.description(),
            result.previousValue(), result.newValue(),
            result.ipAddress(), result.result(), result.createdAt()
        );
    }
}
