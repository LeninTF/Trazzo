package trazzo.back.audit.domain.model.master;

import java.time.LocalDateTime;

import lombok.Getter;

@Getter
public class TenantSettingsRecord {
    private Long id;
    private String tenantSettingId;
    private String dbName;
    private String dbHost;
    private String dbUser;
    private String dbPassword;
    private String userId;
    private String changeReason;
    private LocalDateTime createdAt;

    public TenantSettingsRecord(
            Long id,
            String tenantSettingId,
            String dbName,
            String dbHost,
            String dbUser,
            String dbPassword,
            String userId,
            String changeReason,
            LocalDateTime createdAt) {

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User id is required");
        }

        if (changeReason == null || changeReason.isBlank()) {
            throw new IllegalArgumentException("Change reason is required");
        }
        
        this.id = id;
        this.tenantSettingId = tenantSettingId;
        this.dbName = dbName;
        this.dbHost = dbHost;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
        this.userId = userId;
        this.changeReason = changeReason;
        this.createdAt = createdAt;
    }

    public boolean hasChangeReason() {
        return changeReason != null && !changeReason.isBlank();
    }

    public boolean belongsTo(String userId) {
        return this.userId.equals(userId);
    }

}
