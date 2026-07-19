package trazzo.back.saasglobal.application.dto.result;

import java.time.LocalDateTime;

public record RequestResult(
        Integer id,
        String type,
        String title,
        String message,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        RequestContactResult contact
) {}
