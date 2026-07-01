package trazzo.back.audit.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.audit.application.dto.result.TenantSettingsRecordResult;
import java.time.LocalDateTime;

public record TenantSettingsRecordResponse(
    Long id,
    @JsonProperty("tenant_setting_id") String tenantSettingId,
    @JsonProperty("db_name") String dbName,
    @JsonProperty("db_host") String dbHost,
    @JsonProperty("db_user") String dbUser,
    @JsonProperty("user_id") String userId,
    @JsonProperty("change_reason") String changeReason,
    @JsonProperty("created_at") LocalDateTime createdAt
) {
    public static TenantSettingsRecordResponse from(TenantSettingsRecordResult result) {
        return new TenantSettingsRecordResponse(
            result.id(), result.tenantSettingId(), result.dbName(),
            result.dbHost(), result.dbUser(), result.userId(),
            result.changeReason(), result.createdAt()
        );
    }
}
