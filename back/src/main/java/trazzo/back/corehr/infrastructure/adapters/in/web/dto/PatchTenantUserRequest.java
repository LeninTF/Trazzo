package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PatchTenantUserRequest(
    String name,
    @JsonProperty("father_surname") String fatherSurname,
    @JsonProperty("mother_surname") String motherSurname,
    @JsonProperty("birth_date") String birthDate,
    @JsonProperty("img_url") String imgUrl,
    String email,
    String phone,
    String cargo,
    String estado,
    @JsonProperty("role_id") String roleId,
    @JsonProperty("sede_ids") List<Long> sedeIds,
    @JsonProperty("area_ids") List<Long> areaIds,
    @JsonProperty("departamento_ids") List<Long> departamentoIds
) {}
