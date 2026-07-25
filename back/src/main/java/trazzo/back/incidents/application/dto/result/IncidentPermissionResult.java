package trazzo.back.incidents.application.dto.result;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record IncidentPermissionResult(
        Integer id,
        Integer incidenciaId,
        LocalDate startDate,
        LocalDate endDate,
        int daysGranted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
