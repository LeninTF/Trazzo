package trazzo.back.organization.application.dto.result;

import java.util.List;

public record PaginatedResult<T>(
        List<T> content,
        int page,
        int size,
        long total,
        int totalPages
) {}
