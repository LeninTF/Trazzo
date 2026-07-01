package trazzo.back.audit.application.dto.result;

import java.time.LocalDateTime;

public record TenantSettingsRecordResult(
    Long id,
    String tenantSettingId,
    String dbName,
    String dbHost,
    String dbUser,
    String userId,
    String changeReason,
    LocalDateTime createdAt
) {}
