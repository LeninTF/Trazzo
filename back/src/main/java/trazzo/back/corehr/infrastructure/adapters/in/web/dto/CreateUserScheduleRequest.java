package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record CreateUserScheduleRequest(
        @NotNull @JsonProperty("tenant_user_id") Long tenantUserId,
        @NotNull @JsonProperty("schedule_id") Long scheduleId,
        String description,
        @NotNull @JsonProperty("entry_time") LocalTime entryTime,
        @NotNull @JsonProperty("departure_time") LocalTime departureTime
) {
}
