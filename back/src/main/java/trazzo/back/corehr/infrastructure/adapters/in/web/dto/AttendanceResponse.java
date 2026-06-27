package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.corehr.application.dto.result.AttendanceResult;
import trazzo.back.corehr.domain.model.AttendanceState;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceResponse(
        String id,
        @JsonProperty("tenant_user_id") Long tenantUserId,
        TenantUserBasicInfoResponse tenantUser,
        @JsonProperty("schedule_id") Long scheduleId,
        ScheduleSummaryResponse schedule,
        @JsonProperty("device_id") Long deviceId,
        @JsonProperty("device_code") String deviceCode,
        @JsonProperty("check_in") LocalDateTime checkIn,
        @JsonProperty("check_out") LocalDateTime checkOut,
        @JsonProperty("attendance_date") LocalDate attendanceDate,
        @JsonProperty("minutes_late") int minutesLate,
        AttendanceState state,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt
) {
    public static AttendanceResponse from(AttendanceResult result) {
        var user = result.tenantUser() != null
                ? new TenantUserBasicInfoResponse(result.tenantUser().id(),
                        result.tenantUser().nombre(), result.tenantUser().apellidoPaterno())
                : null;
        var schedule = result.schedule() != null
                ? new ScheduleSummaryResponse(result.schedule().id(), result.schedule().name())
                : null;
        return new AttendanceResponse(result.id(), result.tenantUserId(), user,
                result.scheduleId(), schedule, result.deviceId(), result.deviceCode(),
                result.checkIn(), result.checkOut(), result.attendanceDate(),
                result.minutesLate(), result.state(), result.createdAt(), result.updatedAt());
    }

    public record TenantUserBasicInfoResponse(
            Long id, String nombre,
            @JsonProperty("apellido_paterno") String apellidoPaterno
    ) {
    }

    public record ScheduleSummaryResponse(Long id, String name) {
    }
}
