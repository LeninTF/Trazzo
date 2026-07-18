package trazzo.back.saasglobal.application.dto.result;

import java.time.LocalDateTime;

public record RequestRecordResult(
        Integer id,
        String status,
        String userId,
        String changeReason,
        LocalDateTime createdAt
) {}
