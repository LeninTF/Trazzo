package trazzo.back.saasglobal.application.dto.command;

import java.util.List;

public record CreateSaasUserCommand(
        String documentType,
        String documentValue,
        String name,
        String fatherSurname,
        String motherSurname,
        String email,
        String phone,
        String password,
        List<Integer> roleIds
) {}
