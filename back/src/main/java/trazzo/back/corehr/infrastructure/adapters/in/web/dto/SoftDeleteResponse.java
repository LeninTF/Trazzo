package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import java.time.LocalDateTime;

public record SoftDeleteResponse(
    Long id,
    String status,
    LocalDateTime deletedAt
) {}
