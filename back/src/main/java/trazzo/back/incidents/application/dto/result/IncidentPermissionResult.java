package trazzo.back.incidents.application.dto.result;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record IncidentPermissionResult(
        String id,
        String incidenciaId,
        LocalDate startDate,
        LocalDate endDate,
        int daysGranted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
