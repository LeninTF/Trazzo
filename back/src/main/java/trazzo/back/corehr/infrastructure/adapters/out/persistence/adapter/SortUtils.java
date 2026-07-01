package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import org.springframework.data.domain.Sort;

import java.util.function.Function;

final class SortUtils {

    private SortUtils() {
    }

    static Sort parseSort(String sort, Function<String, String> fieldMapper) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        var parts = sort.split(",");
        var field = fieldMapper.apply(parts[0].trim());
        var direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }

    static NativeSort parseNativeSort(String sort) {
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

    record NativeSort(String field, String direction) {
    }
}
