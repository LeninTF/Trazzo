package trazzo.back.corehr.domain.model;

import trazzo.back.corehr.domain.exception.CoreHrValidationException;
import trazzo.back.corehr.domain.exception.InvalidScheduleException;
import trazzo.back.corehr.domain.specification.ScheduleTimeSpec;

import java.time.LocalTime;

public final class DomainModelValidator {

    private DomainModelValidator() {
    }

    public static Long requireTenantUserId(Long tenantUserId) {
        if (tenantUserId == null) {
            throw new CoreHrValidationException("tenantUserId is required");
        }
        return tenantUserId;
    }

    public static Long requireScheduleTenantUserId(Long tenantUserId) {
        if (tenantUserId == null) {
            throw new InvalidScheduleException("tenantUserId is required");
        }
        return tenantUserId;
    }

    public static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new CoreHrValidationException(fieldName + " is required");
        }
        return value.trim();
    }

    public static String requireScheduleText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidScheduleException(fieldName + " is required");
        }
        return value.trim();
    }

    public static LocalTime requireTime(LocalTime value, String fieldName) {
        if (value == null) {
            throw new InvalidScheduleException(fieldName + " is required");
        }
        return value;
    }

    public static LocalTime requireValidDepartureTime(LocalTime entryTime, LocalTime departureTime) {
        if (departureTime == null) {
            throw new InvalidScheduleException("departureTime is required");
        }
        if (!new ScheduleTimeSpec().isValidScheduleTime(entryTime, departureTime)) {
            throw new InvalidScheduleException("departureTime must be after entryTime");
        }
        return departureTime;
    }

    public static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
