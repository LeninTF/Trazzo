package trazzo.back.audit.application.dto.result;

import trazzo.back.audit.domain.model.tenant.HttpMethod;
import trazzo.back.audit.domain.model.tenant.SystemActions;
import java.time.LocalDateTime;
import java.util.Map;

public record SystemAuditResult(
    Long id,
    String userTenantId,
    SystemActions systemActions,
    String module,
    String entity,
    String entityId,
    HttpMethod httpMethod,
    String endpoint,
    String description,
    Map<String, Object> previousValue,
    Map<String, Object> newValue,
    String ipAddress,
    String result,
    LocalDateTime createdAt
) {}
