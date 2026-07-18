package trazzo.back.saasglobal.infrastructure.adapters.in.web.dto;

import java.util.Map;
import java.util.stream.Collectors;
import trazzo.back.saasglobal.domain.model.iam.RoleMaster;

public record RoleProfileResponse(Integer id, String name, Map<String, Object> permissions) {
    public static RoleProfileResponse fromRoleName(String roleName) {
        return new RoleProfileResponse(0, roleName, Map.of());
    }

    public static RoleProfileResponse from(RoleMaster role) {
        Map<String, Object> permissions = role.getPermissionCodes().stream()
                .collect(Collectors.toMap(code -> code, code -> true));
        return new RoleProfileResponse(role.getId(), role.getName(), permissions);
    }
}
