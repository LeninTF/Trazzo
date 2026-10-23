package trazzo.back.corehr.application.dto.result;

import trazzo.back.corehr.domain.model.AttendanceState;

import java.time.LocalDateTime;

public record SyncItemResult(
        Status status,
        String attendanceId,
        AttendanceState state,
        int minutesLate,
        LocalDateTime checkIn,
        String error,
        Integer offlineEventId
) {
    /** ACCEPTED: registrado ahora; SKIPPED: ya estaba registrado (reenvío); FAILED: rechazado. */
    public enum Status { ACCEPTED, SKIPPED, FAILED }

    public boolean success() {
        return status == Status.ACCEPTED;
    }

    public static SyncItemResult success(String attendanceId, AttendanceState state, int minutesLate,
                                         LocalDateTime checkIn, Integer offlineEventId) {
        return new SyncItemResult(Status.ACCEPTED, attendanceId, state, minutesLate, checkIn, null, offlineEventId);
    }

    public static SyncItemResult failure(String error, Integer offlineEventId) {
        return new SyncItemResult(Status.FAILED, null, null, 0, null, error, offlineEventId);
    }

    public static SyncItemResult skipped(Integer offlineEventId) {
        return new SyncItemResult(Status.SKIPPED, null, null, 0, null, "Duplicate - already processed", offlineEventId);
    }
}
