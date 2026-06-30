package trazzo.back.audit.application.dto.result;

import trazzo.back.audit.domain.model.master.StatusLogin;
import java.time.LocalDateTime;

public record LogInHistoryResult(
    Long id,
    String userId,
    String attemptedEmail,
    StatusLogin status,
    String ipAddress,
    String userAgent,
    LocalDateTime createdAt
) {}
