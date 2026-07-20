package trazzo.back.corehr.application.dto.result;

import trazzo.back.corehr.domain.model.AttendanceState;

import java.time.LocalDateTime;

public record SyncItemResult(
        boolean success,
        String attendanceId,
        AttendanceState state,
        int minutesLate,
        LocalDateTime checkIn,
        String error,
        Integer offlineEventId
) {
    public static SyncItemResult success(String attendanceId, AttendanceState state, int minutesLate,
                                         LocalDateTime checkIn, Integer offlineEventId) {
        return new SyncItemResult(true, attendanceId, state, minutesLate, checkIn, null, offlineEventId);
    }

    public static SyncItemResult failure(String error, Integer offlineEventId) {
        return new SyncItemResult(false, null, null, 0, null, error, offlineEventId);
    }

    public static SyncItemResult skipped(Integer offlineEventId) {
        return new SyncItemResult(false, null, null, 0, null, "Duplicate - already processed", offlineEventId);
    }
}
