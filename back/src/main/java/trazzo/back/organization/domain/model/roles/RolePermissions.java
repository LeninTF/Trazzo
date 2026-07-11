package trazzo.back.organization.domain.model.roles;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class RolePermissions {

    private final String roleId;
    private final String permissionId;
    private final LocalDateTime createdAt;

    private RolePermissions(String roleId, String permissionId, LocalDateTime createdAt) {
        this.roleId = roleId;
        this.permissionId = permissionId;
        this.createdAt = createdAt;
    }

    public static RolePermissions create(String roleId, String permissionId) {
        return new RolePermissions(roleId, permissionId, LocalDateTime.now());
    }

    public static RolePermissions restore(String roleId, String permissionId, LocalDateTime createdAt) {
        return new RolePermissions(roleId, permissionId, createdAt);
    }
}
