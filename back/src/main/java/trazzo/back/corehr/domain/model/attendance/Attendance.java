package trazzo.back.corehr.domain.model.attendance;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.corehr.domain.event.AttendanceCompletedEvent;
import trazzo.back.corehr.domain.event.AttendanceCorrectedEvent;
import trazzo.back.corehr.domain.event.AttendanceRegisteredEvent;
import trazzo.back.corehr.domain.event.CoreHrDomainEvent;
import trazzo.back.corehr.domain.exception.InvalidAttendanceException;
import trazzo.back.corehr.domain.model.AttendanceState;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attendance {

    private String id;
    private Long tenantUserId;
    private Long scheduleId;
    private Long deviceId;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private LocalDate attendanceDate;
    private int minutesLate;
    private AttendanceState state;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @JsonIgnore
    transient List<CoreHrDomainEvent> domainEvents = new ArrayList<>();
    @JsonIgnore
    transient Clock clock = Clock.systemDefaultZone();

    private Attendance(
            String id,
            Long tenantUserId,
            Long scheduleId,
            Long deviceId,
            LocalDateTime checkIn,
            LocalDateTime checkOut,
            LocalDate attendanceDate,
            int minutesLate,
            AttendanceState state,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = normalizeOptionalId(id);
        this.tenantUserId = requireTenantUserId(tenantUserId);
        this.scheduleId = scheduleId;
        this.deviceId = deviceId;
        this.checkIn = checkIn;
        this.checkOut = checkOut;
        this.attendanceDate = requireDate(attendanceDate, "attendanceDate");
        this.minutesLate = minutesLate;
        this.state = requireState(state);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        validateConsistency();
    }

    private void validateConsistency() {
        if (checkIn != null && checkOut != null && checkOut.isBefore(checkIn)) {
            throw new InvalidAttendanceException("checkOut cannot be before checkIn");
        }
    }

    public static Attendance registerCheckIn(
            Long tenantUserId,
            Long scheduleId,
            Long deviceId,
            LocalTime scheduledEntryTime,
            int toleranceMinutes
    ) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalTime actualCheckIn = now.toLocalTime();

        int lateMinutes = 0;
        AttendanceState initialState;
        if (scheduledEntryTime != null) {
            if (actualCheckIn.isAfter(scheduledEntryTime)) {
                int diffMinutes = (int) java.time.Duration.between(scheduledEntryTime, actualCheckIn).toMinutes();
                int effectiveLate = diffMinutes - toleranceMinutes;
                lateMinutes = Math.max(effectiveLate, 0);
                initialState = lateMinutes > 0 ? AttendanceState.TARDANZA : AttendanceState.PUNTUAL;
            } else {
                initialState = AttendanceState.PUNTUAL;
            }
        } else {
            initialState = AttendanceState.PUNTUAL;
        }

        Attendance attendance = new Attendance(
                generateId(),
                tenantUserId,
                scheduleId,
                deviceId,
                now,
                null,
                today,
                lateMinutes,
                initialState,
                now,
                now
        );
        attendance.recordEvent(new AttendanceRegisteredEvent(
                attendance.getId(),
                attendance.getTenantUserId(),
                attendance.getScheduleId(),
                attendance.getDeviceId(),
                now
        ));
        return attendance;
    }

    public static Attendance restore(
            String id,
            Long tenantUserId,
            Long scheduleId,
            Long deviceId,
            LocalDateTime checkIn,
            LocalDateTime checkOut,
            LocalDate attendanceDate,
            int minutesLate,
            AttendanceState state,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new Attendance(id, tenantUserId, scheduleId, deviceId, checkIn, checkOut, attendanceDate, minutesLate, state, createdAt, updatedAt);
    }

    public void registerCheckOut() {
        if (checkOut != null) {
            throw new InvalidAttendanceException("Attendance already has a check-out");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (now.toLocalDate().isBefore(attendanceDate)) {
            throw new InvalidAttendanceException("Check-out date cannot be before attendance date");
        }
        this.checkOut = now;
        touch();
        recordEvent(new AttendanceCompletedEvent(id, tenantUserId, checkOut, state, updatedAt));
    }

    public void correct(int minutesLate, AttendanceState newState) {
        if (minutesLate < 0) {
            throw new InvalidAttendanceException("minutesLate must be non-negative");
        }
        if (newState == null) {
            throw new InvalidAttendanceException("newState is required");
        }
        AttendanceState previousState = this.state;
        int previousMinutesLate = this.minutesLate;
        this.minutesLate = minutesLate;
        this.state = newState;
        touch();
        recordEvent(new AttendanceCorrectedEvent(
                id, tenantUserId, previousState, newState, previousMinutesLate, minutesLate, updatedAt
        ));
    }

    public boolean isComplete() {
        return checkIn != null && checkOut != null;
    }

    @JsonIgnore
    public List<CoreHrDomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public List<CoreHrDomainEvent> pullDomainEvents() {
        List<CoreHrDomainEvent> events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now(clock);
    }

    private void recordEvent(CoreHrDomainEvent event) {
        this.domainEvents.add(event);
    }

    private static String generateId() {
        return UUID.randomUUID().toString();
    }

    private static Long requireTenantUserId(Long tenantUserId) {
        if (tenantUserId == null) {
            throw new InvalidAttendanceException("tenantUserId is required");
        }
        return tenantUserId;
    }

    private static LocalDate requireDate(LocalDate value, String fieldName) {
        if (value == null) {
            throw new InvalidAttendanceException(fieldName + " is required");
        }
        return value;
    }

    private static AttendanceState requireState(AttendanceState state) {
        if (state == null) {
            throw new InvalidAttendanceException("state is required");
        }
        return state;
    }

    private static String normalizeOptionalId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
