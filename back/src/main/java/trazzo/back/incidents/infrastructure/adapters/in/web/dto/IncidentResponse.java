package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.incidents.application.dto.result.IncidentResult;
import trazzo.back.incidents.domain.model.IncidentState;

import java.time.LocalDateTime;
import java.util.List;

public record IncidentResponse(
        String id,
        @JsonProperty("tenant_user_id") String tenantUserId,
        @JsonProperty("incidencia_type_id") String incidenciaTypeId,
        IncidentState state,
        String comment,
        String rejectionReason,
        IncidentTypeResponse tipo,
        IncidentPermissionResponse permiso,
        List<IncidentEvidenceResponse> evidencias,
        @JsonProperty("tenant_user") TenantUserBasicInfoResponse tenantUser,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt
) {
    public static IncidentResponse from(IncidentResult result) {
        IncidentTypeResponse tipoResp = result.tipo() != null
                ? IncidentTypeResponse.from(result.tipo())
                : null;
        IncidentPermissionResponse permisoResp = result.permiso() != null
                ? IncidentPermissionResponse.from(result.permiso())
                : null;
        List<IncidentEvidenceResponse> evidResp = result.evidencias() != null
                ? result.evidencias().stream().map(IncidentEvidenceResponse::from).toList()
                : List.of();
        TenantUserBasicInfoResponse userResp = result.tenantUser() != null
                ? TenantUserBasicInfoResponse.from(result.tenantUser())
                : null;

        return new IncidentResponse(result.id(), result.tenantUserId(), result.incidenciaTypeId(),
                result.state(), result.comment(), result.rejectionReason(), tipoResp,
                permisoResp, evidResp, userResp, result.createdAt(), result.updatedAt());
    }

    public record TenantUserBasicInfoResponse(
            String id, String nombre,
            @JsonProperty("apellido_paterno") String apellidoPaterno,
            @JsonProperty("apellido_materno") String apellidoMaterno,
            String email
    ) {
        static TenantUserBasicInfoResponse from(IncidentResult.TenantUserBasicInfoResult result) {
            return new TenantUserBasicInfoResponse(result.id(), result.nombre(),
                    result.apellidoPaterno(), result.apellidoMaterno(), result.email());
        }
    }
}
