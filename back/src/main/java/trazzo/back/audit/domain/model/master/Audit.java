package trazzo.back.audit.domain.model.master;

import java.time.LocalDateTime;
import java.util.Map;

public class Audit {
    private String id;
    private String entity;
    private String entityId;
    private Action action;
    private String userId;
    private String endpoint;
    private String ipAdress;
    private String userAgent;
    private Map<String, Object> previousValue;
    private Map<String, Object> newValue;
    private LocalDateTime createdAt;

    public Audit() {}

    public static Audit restore(
            String id, String entity, String entityId, Action action,
            String userId, String endpoint, String ipAdress,
            String userAgent, Map<String, Object> previousValue,
            Map<String, Object> newValue, LocalDateTime createdAt) {
        Audit audit = new Audit();
        audit.id = id;
        audit.entity = entity;
        audit.entityId = entityId;
        audit.action = action;
        audit.userId = userId;
        audit.endpoint = endpoint;
        audit.ipAdress = ipAdress;
        audit.userAgent = userAgent;
        audit.previousValue = previousValue;
        audit.newValue = newValue;
        audit.createdAt = createdAt;
        return audit;
    }

    public String getId() { return id; }
    public String getEntity() { return entity; }
    public String getEntityId() { return entityId; }
    public Action getAction() { return action; }
    public String getUserId() { return userId; }
    public String getEndpoint() { return endpoint; }
    public String getIpAdress() { return ipAdress; }
    public String getUserAgent() { return userAgent; }
    public Map<String, Object> getPreviousValue() { return previousValue; }
    public Map<String, Object> getNewValue() { return newValue; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
