package trazzo.back.corehr.application.dto.command;

import java.util.List;

public record CreateTenantUserCommand(
    String documentType,
    String documentValue,
    String name,
    String fatherSurname,
    String motherSurname,
    String birthDate,
    String imgUrl,
    String email,
    String phone,
    String roleId,
    List<Long> sedeIds,
    List<Long> areaIds,
    List<Long> departamentoIds
) {}
