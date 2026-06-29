package trazzo.back.corehr.application.dto.result;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record NonWorkingDayResult(
        Long id,
        LocalDate date,
        String description,
        boolean isRecurring,
        LocalDateTime createdAt
) {
}
