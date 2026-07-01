package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalTime;

public record PatchScheduleRequest(
        String name,
        String description,
        @JsonProperty("entry_time") LocalTime entryTime,
        @JsonProperty("departure_time") LocalTime departureTime
) {
}
