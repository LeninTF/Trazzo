package trazzo.back.organization.application.dto.result;

import java.util.List;

public record PaginatedResult<T>(
        List<T> content,
        int page,
        int size,
        long total,
        int totalPages
) {
    public static <T> PaginatedResult<T> of(List<T> content, int page, int size, long total) {
        int pages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PaginatedResult<>(content, page, size, total, pages);
    }
}
