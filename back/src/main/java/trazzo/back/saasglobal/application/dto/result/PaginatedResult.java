package trazzo.back.saasglobal.application.dto.result;

import java.util.List;

// Field names match the frontend's shared PageResponse<T> (front/src/app/api/types.ts):
// content/page/size/totalElements/totalPages.
public record PaginatedResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PaginatedResult<T> of(List<T> content, int page, int size, long totalElements) {
        int pages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return new PaginatedResult<>(content, page, size, totalElements, pages);
    }
}
