package trazzo.back.organization.infrastructure.adapters.out.persistence;

import org.springframework.data.domain.Sort;

public final class OrgPersistenceUtils {

    private OrgPersistenceUtils() {}

    public static Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) return Sort.by(Sort.Direction.ASC, "name");
        var parts = sort.split(",");
        var direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, mapField(parts[0].trim()));
    }

    public static String mapField(String field) {
        return switch (field) {
            case "created_at", "createdAt" -> "createdAt";
            case "updated_at", "updatedAt" -> "updatedAt";
            default -> "name";
        };
    }

    public static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
