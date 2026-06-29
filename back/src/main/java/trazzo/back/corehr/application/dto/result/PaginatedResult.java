package trazzo.back.corehr.application.dto.result;

import java.util.List;

public record PaginatedResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
}
