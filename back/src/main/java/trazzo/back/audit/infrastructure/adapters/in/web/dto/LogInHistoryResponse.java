package trazzo.back.audit.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.audit.application.dto.result.LogInHistoryResult;
import trazzo.back.audit.domain.model.master.StatusLogin;
import java.time.LocalDateTime;

public record LogInHistoryResponse(
    String id,
    @JsonProperty("user_id") String userId,
    @JsonProperty("attempted_email") String attemptedEmail,
    StatusLogin status,
    @JsonProperty("ip_address") String ipAddress,
    @JsonProperty("user_agent") String userAgent,
    @JsonProperty("created_at") LocalDateTime createdAt
) {
    public static LogInHistoryResponse from(LogInHistoryResult result) {
        return new LogInHistoryResponse(
            result.id(), result.userId(), result.attemptedEmail(),
            result.status(), result.ipAddress(), result.userAgent(),
            result.createdAt()
        );
    }
}
