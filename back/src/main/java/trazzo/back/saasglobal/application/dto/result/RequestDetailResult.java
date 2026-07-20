package trazzo.back.saasglobal.application.dto.result;

import java.time.LocalDateTime;
import java.util.List;

public record RequestDetailResult(
        Integer id,
        String type,
        String title,
        String message,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        RequestContactResult contact,
        List<RequestCommentResult> comments,
        List<RequestRecordResult> history
) {}
