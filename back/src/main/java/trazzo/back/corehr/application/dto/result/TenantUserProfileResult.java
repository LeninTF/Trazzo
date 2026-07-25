package trazzo.back.corehr.application.dto.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public record TenantUserProfileResult(
    Long id,
    String email,
    String phone,
    String estado,
    @JsonProperty("must_change_password") boolean mustChangePassword,
    @JsonProperty("created_at") LocalDateTime createdAt,
    @JsonProperty("updated_at") LocalDateTime updatedAt,
    PersonaResult persona,
    RoleInfoResult rol,
    List<OrgAssignment> sedes,
    List<OrgAssignment> areas,
    List<OrgAssignment> departamentos
) {
  public record PersonaResult(
      Integer id,
      @JsonProperty("document_type") String documentType,
      @JsonProperty("document_value") String documentValue,
      String name,
      @JsonProperty("father_surname") String fatherSurname,
      @JsonProperty("mother_surname") String motherSurname,
      @JsonProperty("birth_date") String birthDate,
      @JsonProperty("img_url") String imgUrl
  ) {}

  public record RoleInfoResult(
      String id,
      String name
  ) {}

  public record OrgAssignment(
      Long id,
      String nombre
  ) {}
}
