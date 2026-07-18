package trazzo.back.shared.util;

import org.springframework.data.domain.Sort;

import java.util.Map;
import java.util.function.Function;

public final class SortUtils {

    private SortUtils() {
    }

    public static final Map<String, String> AUDIT_SORT_FIELD_MAP = Map.of(
            "createdAt", "created_at",
            "ipAddress", "ip_address",
            "entityId", "entity_id"
    );

    public static Sort parseSort(String sort, Function<String, String> fieldMapper) {
        return parseSort(sort, fieldMapper, "createdAt");
    }

    public static Sort parseSort(String sort, Function<String, String> fieldMapper, String defaultField) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, defaultField);
        }
        var parts = sort.split(",");
        var field = fieldMapper.apply(parts[0].trim());
        var direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }

    public static NativeSort parseNativeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return new NativeSort("a.created_at", "DESC");
        }
        var parts = sort.split(",");
        var direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
                ? "DESC" : "ASC";
        var field = switch (parts[0].trim()) {
            case "attendance_date", "attendanceDate" -> "a.attendance_date";
            case "check_in", "checkIn" -> "a.check_in";
            case "check_out", "checkOut" -> "a.check_out";
            case "minutes_late", "minutesLate" -> "a.minutes_late";
            case "state" -> "a.state";
            case "updated_at", "updatedAt" -> "a.updated_at";
            default -> "a.created_at";
        };
        return new NativeSort(field, direction);
    }

    public record NativeSort(String field, String direction) {
    }
}
