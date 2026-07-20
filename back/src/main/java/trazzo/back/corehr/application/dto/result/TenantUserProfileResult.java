package trazzo.back.corehr.application.dto.result;

import java.time.LocalDateTime;
import java.util.List;

public record TenantUserProfileResult(
    Long id,
    String email,
    String phone,
    String estado,
    boolean mustChangePassword,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    PersonaResult persona,
    RoleInfoResult rol,
    List<OrgAssignment> sedes,
    List<OrgAssignment> areas,
    List<OrgAssignment> departamentos
) {
  public record PersonaResult(
      Integer id,
      String documentType,
      String documentValue,
      String name,
      String fatherSurname,
      String motherSurname,
      String birthDate
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
