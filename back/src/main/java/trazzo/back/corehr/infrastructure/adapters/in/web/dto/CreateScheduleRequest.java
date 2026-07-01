package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record CreateScheduleRequest(
        @NotNull @JsonProperty("shift_id") Long shiftId,
        @NotBlank String name,
        String description,
        @NotNull @JsonProperty("entry_time") LocalTime entryTime,
        @NotNull @JsonProperty("departure_time") LocalTime departureTime
) {
}
