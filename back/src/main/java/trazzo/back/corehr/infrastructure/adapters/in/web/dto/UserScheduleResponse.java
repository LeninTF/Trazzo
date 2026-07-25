package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.corehr.application.dto.result.ShiftResult;
import trazzo.back.corehr.application.dto.result.UserScheduleResult;

import java.time.DayOfWeek;
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
        @JsonProperty("updated_at") LocalDateTime updatedAt,
        TenantUserSummaryResponse tenantUser
) {
    public static UserScheduleResponse from(UserScheduleResult result) {
        var schedule = result.schedule() != null
                ? new ScheduleSummaryResponse(result.schedule().id(), result.schedule().name(),
                        result.schedule().entryTime(), result.schedule().departureTime(),
                        result.schedule().daysOfWeek() != null ? result.schedule().daysOfWeek() : java.util.List.<DayOfWeek>of())
                : null;
        var tenantUser = result.tenantUser() != null
                ? new TenantUserSummaryResponse(result.tenantUser().id(),
                        result.tenantUser().name(), result.tenantUser().fatherSurname(), result.tenantUser().motherSurname(),
                        result.tenantUser().sede(), result.tenantUser().area(), result.tenantUser().department())
                : null;
        return new UserScheduleResponse(result.id(), result.tenantUserId(), result.scheduleId(),
                schedule, result.description(), result.entryTime(), result.departureTime(),
                result.createdAt(), result.updatedAt(), tenantUser);
    }

    public record ScheduleSummaryResponse(
            Long id,
            String name,
            @JsonProperty("entry_time") LocalTime entryTime,
            @JsonProperty("departure_time") LocalTime departureTime,
            @JsonProperty("days_of_week") java.util.List<DayOfWeek> daysOfWeek
    ) {
        public ScheduleSummaryResponse(Long id, String name) {
            this(id, name, null, null, java.util.List.of());
        }
    }

    public record TenantUserSummaryResponse(
            Long id,
            String name,
            @JsonProperty("father_surname") String fatherSurname,
            @JsonProperty("mother_surname") String motherSurname,
            String sede,
            String area,
            String department
    ) {
    }
}
