package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateTenantUserRequest(
    @NotBlank @JsonProperty("document_type") String documentType,
    @NotBlank @JsonProperty("document_value") String documentValue,
    @NotBlank String name,
    @NotBlank @JsonProperty("father_surname") String fatherSurname,
    @JsonProperty("mother_surname") String motherSurname,
    @JsonProperty("birth_date") String birthDate,
    @JsonProperty("img_url") String imgUrl,
    @NotBlank String email,
    String phone,
    @NotNull @JsonProperty("role_id") String roleId,
    @JsonProperty("sede_ids") List<Long> sedeIds,
    @JsonProperty("area_ids") List<Long> areaIds,
    @JsonProperty("departamento_ids") List<Long> departamentoIds
) {}
