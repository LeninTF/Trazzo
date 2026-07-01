package trazzo.back.audit.domain.model.tenant;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.Getter;

@Getter

public class SystemAudit {
    private Long id;
    private String userTenantId;
    private SystemActions systemActions;
    private String module;
    private String entity;
    private String entityId;
    private HttpMethod httpMethod;
    private String endpoint;
    private String description;
    private Map<String, Object> previousValue;
    private Map<String, Object> newValue;
    private String ipAddress;
    private String result;
    private LocalDateTime createdAt;

    public SystemAudit(
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
            LocalDateTime createdAt) {
        this.id = id;
        this.userTenantId = userTenantId;
        this.systemActions = systemActions;
        this.module = module;
        this.entity = entity;
        this.entityId = entityId;
        this.httpMethod = httpMethod;
        this.endpoint = endpoint;
        this.description = description;
        this.previousValue = previousValue;
        this.newValue = newValue;
        this.ipAddress = ipAddress;
        this.result = result;
        this.createdAt = createdAt;
    }

}
