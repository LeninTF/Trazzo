package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.corehr.application.dto.result.ShiftResult;

import java.time.LocalDateTime;
import java.util.List;

public record ShiftResponse(
        Long id,
        String name,
        String description,
        List<ScheduleSummaryResponse> schedules,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt
) {
    public static ShiftResponse from(ShiftResult result) {
        var schedules = result.schedules().stream()
                .map(s -> new ScheduleSummaryResponse(s.id(), s.name()))
                .toList();
        return new ShiftResponse(result.id(), result.name(), result.description(),
                schedules, result.createdAt(), result.updatedAt());
    }

    public record ScheduleSummaryResponse(
            Long id,
            String name
    ) {
    }
}
