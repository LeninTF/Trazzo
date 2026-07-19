package trazzo.back.saasglobal.application.dto.result;

import java.time.LocalDateTime;

public record RequestCommentResult(
        Integer id,
        String comment,
        String authorUserId,
        LocalDateTime createdAt
) {}
