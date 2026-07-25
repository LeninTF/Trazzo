package trazzo.back.corehr.application.dto.result;

import java.time.LocalDateTime;

public record SoftDeleteResult(
    Long id,
    String status,
    LocalDateTime deletedAt
) {}
