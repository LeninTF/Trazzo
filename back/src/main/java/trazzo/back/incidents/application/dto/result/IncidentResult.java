package trazzo.back.incidents.application.dto.result;

import trazzo.back.incidents.domain.model.IncidentState;

import java.time.LocalDateTime;
import java.util.List;

public record IncidentResult(
        Integer id,
        Integer tenantUserId,
        Integer incidenciaTypeId,
        IncidentState state,
        String comment,
        String rejectionReason,
        IncidentTypeResult tipo,
        IncidentPermissionResult permiso,
        List<IncidentEvidenceResult> evidencias,
        TenantUserBasicInfoResult tenantUser,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record TenantUserBasicInfoResult(Long id, String nombre, String apellidoPaterno,
                                             String apellidoMaterno, String email) {
    }
}
