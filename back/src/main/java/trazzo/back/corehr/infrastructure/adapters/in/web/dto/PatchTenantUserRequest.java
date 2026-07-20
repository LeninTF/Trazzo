package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import java.util.List;

public record PatchTenantUserRequest(
    String name,
    String fatherSurname,
    String motherSurname,
    String birthDate,
    String imgUrl,
    String email,
    String phone,
    String cargo,
    String estado,
    String roleId,
    List<Long> sedeIds,
    List<Long> areaIds,
    List<Long> departamentoIds
) {}
