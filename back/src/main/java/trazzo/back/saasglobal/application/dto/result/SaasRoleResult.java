package trazzo.back.saasglobal.application.dto.result;

import java.util.List;

public record SaasRoleResult(
        Integer id,
        String name,
        String displayName,
        String description,
        List<String> permissions,
        boolean systemManaged
) {}
