package trazzo.back.corehr.application.dto.result;

import trazzo.back.corehr.domain.model.AttendanceState;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AttendanceResult(
        String id,
        Long tenantUserId,
        TenantUserBasicInfo tenantUser,
        Long scheduleId,
        ShiftResult.ScheduleSummary schedule,
        Long deviceId,
        String deviceCode,
        LocalDateTime checkIn,
        LocalDateTime checkOut,
        LocalDate attendanceDate,
        int minutesLate,
        AttendanceState state,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record TenantUserBasicInfo(String id, String nombre, String apellidoPaterno) {
    }
}
