package trazzo.back.corehr.application.dto.result;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record ScheduleResult(
        Long id,
        Long shiftId,
        ShiftSummary shift,
        String name,
        String description,
        LocalTime entryTime,
        LocalTime departureTime,
        List<ToleranciaResult> tolerancias,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record ShiftSummary(Long id, String name) {
    }
}
