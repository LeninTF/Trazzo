package trazzo.back.saasglobal.domain.model.iam;

import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.saasglobal.domain.exception.UserValidationException;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleMaster {

    private Integer id;
    private String name;
    private String displayName;
    private String description;
    private List<String> permissionCodes;

    private RoleMaster(Integer id, String name, String displayName, String description, List<String> permissionCodes) {
        this.id = id;
        this.name = requireText(name, "name");
        this.displayName = displayName;
        this.description = description;
        this.permissionCodes = permissionCodes != null ? List.copyOf(permissionCodes) : List.of();
    }

    public static RoleMaster create(String name, String displayName, String description) {
        return new RoleMaster(null, name, displayName, description, List.of());
    }

    public static RoleMaster restore(Integer id, String name, String displayName, String description,
                                     List<String> permissionCodes) {
        return new RoleMaster(id, name, displayName, description, permissionCodes);
    }

    public void update(String name, String displayName, String description) {
        this.name = requireText(name, "name");
        this.displayName = displayName;
        this.description = description;
    }

    public void grantPermissions(List<String> codes) {
        for (String code : codes) {
            if (!PermissionCatalog.ALL_CODES.contains(code)) {
                throw new UserValidationException("Unknown permission code: " + code);
            }
        }
        this.permissionCodes = List.copyOf(codes);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new UserValidationException(fieldName + " is required");
        }
        return value.trim();
    }
}
