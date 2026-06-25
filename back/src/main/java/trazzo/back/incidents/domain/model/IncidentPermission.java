package trazzo.back.incidents.domain.model;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.incidents.domain.exception.InvalidIncidentPermissionException;
import trazzo.back.incidents.domain.specification.IncidentPermissionSpec;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IncidentPermission {

    private String id;
    private String incidentId;
    private LocalDate startDate;
    private LocalDate endDate;
    private int daysGranted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    transient Clock clock = Clock.systemDefaultZone();

    private IncidentPermission(
            String id,
            String incidentId,
            LocalDate startDate,
            LocalDate endDate,
            int daysGranted,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = normalizeOptionalId(id);
        this.incidentId = requireText(incidentId, "incidentId");
        this.startDate = requireDate(startDate, "startDate");
        this.endDate = requireValidEndDate(startDate, endDate);
        this.daysGranted = requirePositiveDays(daysGranted);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static IncidentPermission create(
            String incidentId,
            LocalDate startDate,
            LocalDate endDate,
            int daysGranted
    ) {
        LocalDateTime now = LocalDateTime.now();
        return new IncidentPermission(null, incidentId, startDate, endDate, daysGranted, now, now);
    }

    public static IncidentPermission restore(
            String id,
            String incidentId,
            LocalDate startDate,
            LocalDate endDate,
            int daysGranted,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new IncidentPermission(id, incidentId, startDate, endDate, daysGranted, createdAt, updatedAt);
    }

    public void reschedule(LocalDate startDate, LocalDate endDate, int daysGranted) {
        this.startDate = requireDate(startDate, "startDate");
        this.endDate = requireValidEndDate(startDate, endDate);
        this.daysGranted = requirePositiveDays(daysGranted);
        touch();
    }

    public boolean belongsTo(String incidentId) {
        return this.incidentId.equals(requireText(incidentId, "incidentId"));
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now(clock);
    }

    private static LocalDate requireDate(LocalDate value, String fieldName) {
        if (value == null) {
            throw new InvalidIncidentPermissionException(fieldName + " is required");
        }
        return value;
    }

    private static LocalDate requireValidEndDate(LocalDate startDate, LocalDate endDate) {
        if (!new IncidentPermissionSpec().hasValidPeriod(startDate, endDate)) {
            throw new InvalidIncidentPermissionException("endDate cannot be before startDate");
        }
        return endDate;
    }

    private static int requirePositiveDays(int daysGranted) {
        if (!new IncidentPermissionSpec().hasValidDaysGranted(daysGranted)) {
            throw new InvalidIncidentPermissionException("daysGranted must be greater than zero");
        }
        return daysGranted;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidIncidentPermissionException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptionalId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
