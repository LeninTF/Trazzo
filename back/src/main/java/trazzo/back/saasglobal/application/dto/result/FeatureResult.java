package trazzo.back.saasglobal.application.dto.result;

import java.time.LocalDateTime;

public record FeatureResult(
        Integer id,
        String name,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
