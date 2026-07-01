package trazzo.back.corehr.application.dto.result;

import java.time.LocalDateTime;
import java.util.List;

public record ShiftResult(
        Long id,
        String name,
        String description,
        List<ScheduleSummary> schedules,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record ScheduleSummary(Long id, String name) {
    }
}
