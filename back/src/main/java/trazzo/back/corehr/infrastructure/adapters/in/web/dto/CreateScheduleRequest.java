package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public record CreateScheduleRequest(
        @NotNull @JsonProperty("shift_id") Long shiftId,
        @NotBlank String name,
        String description,
        @NotNull @JsonProperty("entry_time") LocalTime entryTime,
        @NotNull @JsonProperty("departure_time") LocalTime departureTime,
        @JsonProperty("days_of_week") List<DayOfWeek> daysOfWeek
) {
}
