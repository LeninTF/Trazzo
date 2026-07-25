package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.corehr.application.dto.result.ShiftResult;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
                .map(s -> new ScheduleSummaryResponse(s.id(), s.name(), s.entryTime(), s.departureTime(),
                        s.daysOfWeek() != null ? s.daysOfWeek() : List.<DayOfWeek>of()))
                .toList();
        return new ShiftResponse(result.id(), result.name(), result.description(),
                schedules, result.createdAt(), result.updatedAt());
    }

    public record ScheduleSummaryResponse(
            Long id,
            String name,
            @JsonProperty("entry_time") LocalTime entryTime,
            @JsonProperty("departure_time") LocalTime departureTime,
            @JsonProperty("days_of_week") List<DayOfWeek> daysOfWeek
    ) {
        public ScheduleSummaryResponse(Long id, String name) {
            this(id, name, null, null, List.of());
        }
    }
}
