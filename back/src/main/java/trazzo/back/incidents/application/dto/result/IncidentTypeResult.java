package trazzo.back.incidents.application.dto.result;

import java.time.LocalDateTime;

public record IncidentTypeResult(
        String id,
        String nombre,
        String descripcion,
        boolean activo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
