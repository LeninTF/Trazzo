package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.corehr.application.dto.result.TenantContactResult;

import java.time.LocalDateTime;

public record TenantContactResponse(
        Long id,
        @JsonProperty("tenant_user_id") Long tenantUserId,
        String type,
        TenantUserBasicInfoResponse tenantUser,
        @JsonProperty("created_at") LocalDateTime createdAt,
        @JsonProperty("updated_at") LocalDateTime updatedAt,
        @JsonProperty("deleted_at") LocalDateTime deletedAt
) {
    public static TenantContactResponse from(TenantContactResult result) {
        var user = result.tenantUser() != null
                ? new TenantUserBasicInfoResponse(result.tenantUser().id(),
                        result.tenantUser().nombre(), result.tenantUser().apellidoPaterno(),
                        result.tenantUser().apellidoMaterno(), result.tenantUser().email(),
                        result.tenantUser().phone())
                : null;
        return new TenantContactResponse(result.id(), result.tenantUserId(), result.type(),
                user, result.createdAt(), result.updatedAt(), result.deletedAt());
    }

    public record TenantUserBasicInfoResponse(
            String id, String nombre,
            @JsonProperty("apellido_paterno") String apellidoPaterno,
            @JsonProperty("apellido_materno") String apellidoMaterno,
            String email, String phone
    ) {
    }
}
