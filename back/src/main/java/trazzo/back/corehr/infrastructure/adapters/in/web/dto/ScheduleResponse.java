package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.corehr.application.dto.result.ScheduleResult;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record ScheduleResponse(
        Long id,
        @JsonProperty("shift_id") Long shiftId,
        ShiftSummaryResponse shift,
        String name,
        String description,
        @JsonProperty("entry_time") LocalTime entryTime,
        @JsonProperty("departure_time") LocalTime departureTime,
        @JsonProperty("days_of_week") List<DayOfWeek> daysOfWeek,
        List<ToleranciaResponse> tolerancias,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt
) {
    public static ScheduleResponse from(ScheduleResult result) {
        var tolerancias = result.tolerancias() != null
                ? result.tolerancias().stream().map(ToleranciaResponse::from).toList()
                : List.<ToleranciaResponse>of();
        var shift = result.shift() != null
                ? new ShiftSummaryResponse(result.shift().id(), result.shift().name())
                : null;
        var days = result.daysOfWeek() != null ? result.daysOfWeek() : List.<DayOfWeek>of();
        return new ScheduleResponse(result.id(), result.shiftId(), shift,
                result.name(), result.description(), result.entryTime(), result.departureTime(),
                days, tolerancias, result.createdAt(), result.updatedAt());
    }

    public record ShiftSummaryResponse(Long id, String name) {
    }
}
