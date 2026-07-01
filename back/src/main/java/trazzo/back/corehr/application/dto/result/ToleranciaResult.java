package trazzo.back.corehr.application.dto.result;

import trazzo.back.corehr.domain.model.ToleranciaType;

import java.time.LocalDateTime;

public record ToleranciaResult(
        Long id,
        Long scheduleId,
        String name,
        ToleranciaType type,
        Integer minutes,
        String description,
        boolean activo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
