package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.corehr.application.dto.result.UserScheduleResult;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record UserScheduleResponse(
        Long id,
        @JsonProperty("tenant_user_id") Long tenantUserId,
        @JsonProperty("schedule_id") Long scheduleId,
        ScheduleSummaryResponse schedule,
        String description,
        @JsonProperty("entry_time") LocalTime entryTime,
        @JsonProperty("departure_time") LocalTime departureTime,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt
) {
    public static UserScheduleResponse from(UserScheduleResult result) {
        var schedule = result.schedule() != null
                ? new ScheduleSummaryResponse(result.schedule().id(), result.schedule().name())
                : null;
        return new UserScheduleResponse(result.id(), result.tenantUserId(), result.scheduleId(),
                schedule, result.description(), result.entryTime(), result.departureTime(),
                result.createdAt(), result.updatedAt());
    }

    public record ScheduleSummaryResponse(Long id, String name) {
    }
}
