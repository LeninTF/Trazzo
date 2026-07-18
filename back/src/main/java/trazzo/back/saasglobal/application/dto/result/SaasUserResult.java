package trazzo.back.saasglobal.application.dto.result;

import java.time.LocalDateTime;
import java.util.List;

public record SaasUserResult(
        String id,
        String email,
        String phone,
        boolean mustChangePassword,
        LocalDateTime createdAt,
        PersonSummary person,
        List<RoleTag> roles
) {
    public record PersonSummary(
            Integer id, String imgUrl, String documentType, String documentValue,
            String name, String fatherSurname, String motherSurname
    ) {}

    public record RoleTag(Integer id, String name, String description) {}
}
