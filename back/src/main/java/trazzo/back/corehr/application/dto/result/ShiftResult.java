package trazzo.back.corehr.application.dto.result;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record ShiftResult(
        Long id,
        String name,
        String description,
        List<ScheduleSummary> schedules,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record ScheduleSummary(
            Long id,
            String name,
            LocalTime entryTime,
            LocalTime departureTime,
            List<DayOfWeek> daysOfWeek
    ) {
        public ScheduleSummary(Long id, String name) {
            this(id, name, null, null, List.of());
        }
    }
}
