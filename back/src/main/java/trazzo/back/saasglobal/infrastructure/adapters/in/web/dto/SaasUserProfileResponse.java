package trazzo.back.saasglobal.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import trazzo.back.saasglobal.application.dto.result.SaasUserResult;

// Field names/shape match the frontend's pre-existing MasterUserProfile contract
// (front/src/app/api/types.ts) verbatim, including its snake_case keys.
public record SaasUserProfileResponse(
        String id,
        String email,
        String phone,
        @JsonProperty("tenant_id") String tenantId,
        @JsonProperty("must_change_password") boolean mustChangePassword,
        @JsonProperty("created_at") LocalDateTime createdAt,
        Persona persona,
        @JsonProperty("MetodoRecuperacion") List<Object> metodoRecuperacion,
        List<RoleTag> roles,
        @JsonProperty("tenant_info") Object tenantInfo
) {
    public record Persona(
            Integer id,
            @JsonProperty("img_url") String imgUrl,
            @JsonProperty("document_type") String documentType,
            @JsonProperty("document_value") String documentValue,
            String name,
            @JsonProperty("father_surname") String fatherSurname,
            @JsonProperty("mother_surname") String motherSurname
    ) {}

    public record RoleTag(Integer id, String name, String descripcion) {}

    public static SaasUserProfileResponse from(SaasUserResult result) {
        Persona persona = result.person() != null
                ? new Persona(result.person().id(), result.person().imgUrl(), result.person().documentType(),
                        result.person().documentValue(), result.person().name(),
                        result.person().fatherSurname(), result.person().motherSurname())
                : null;
        List<RoleTag> roleTags = result.roles().stream()
                .map(r -> new RoleTag(r.id(), r.name(), r.description()))
                .toList();
        return new SaasUserProfileResponse(result.id(), result.email(), result.phone(), null,
                result.mustChangePassword(), result.createdAt(), persona, List.of(), roleTags, null);
    }
}
