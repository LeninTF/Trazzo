package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.corehr.application.dto.result.ToleranciaResult;
import trazzo.back.corehr.domain.model.ToleranciaType;

import java.time.LocalDateTime;

public record ToleranciaResponse(
        Long id,
        @JsonProperty("schedule_id") Long scheduleId,
        String name,
        ToleranciaType type,
        Integer minutes,
        String description,
        boolean activo,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt
) {
    public static ToleranciaResponse from(ToleranciaResult result) {
        return new ToleranciaResponse(result.id(), result.scheduleId(), result.name(),
                result.type(), result.minutes(), result.description(), result.activo(),
                result.createdAt(), result.updatedAt());
    }
}
