package trazzo.back.saasglobal.infrastructure.adapters.in.web.dto;

import java.util.Map;

public record RoleProfileResponse(Integer id, String name, Map<String, Object> permissions) {
    public static RoleProfileResponse fromRoleName(String roleName) {
        return new RoleProfileResponse(0, roleName, Map.of());
    }
}
