package trazzo.back.saasglobal.domain.model.iam;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.saasglobal.domain.exception.UserValidationException;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleMaster {

    private Integer id;
    private String name;
    private String description;

    private RoleMaster(Integer id, String name, String description) {
        this.id = id;
        this.name = requireText(name, "name");
        this.description = description;
    }

    public static RoleMaster create(String name, String description) {
        return new RoleMaster(null, name, description);
    }

    public static RoleMaster restore(Integer id, String name, String description) {
        return new RoleMaster(id, name, description);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new UserValidationException(fieldName + " is required");
        }
        return value.trim();
    }
}
