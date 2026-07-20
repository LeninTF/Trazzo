package trazzo.back.corehr.application.dto.command;

import java.util.List;

public record PatchTenantUserCommand(
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
