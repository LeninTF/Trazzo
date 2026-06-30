package trazzo.back.audit.infrastructure.adapters.out.persistence.adapter;

import org.springframework.data.domain.Sort;

import java.util.function.Function;

public final class SortUtils {

    private SortUtils() {
    }

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
}
