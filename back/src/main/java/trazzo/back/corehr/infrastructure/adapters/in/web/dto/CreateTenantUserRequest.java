package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateTenantUserRequest(
    @NotBlank String documentType,
    @NotBlank String documentValue,
    @NotBlank String name,
    @NotBlank String fatherSurname,
    @NotBlank String motherSurname,
    String birthDate,
    String imgUrl,
    @NotBlank String email,
    String phone,
    @NotNull String roleId,
    List<Long> sedeIds,
    List<Long> areaIds,
    List<Long> departamentoIds
) {}
