package trazzo.back.saasglobal.application.dto.result;

import java.time.LocalDateTime;

public record HoldingResult(
        Integer id,
        String taxId,
        String legalName,
        String type,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
