package trazzo.back.organization.domain.model.roles;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.organization.domain.exception.OrgValidationException;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Permissions {

    private String id;
    private String name;
    private String description;
    private String masterFeaturesCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Permissions(String id, String name, String description, String masterFeaturesCode,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = requireText(name, "name");
        this.description = description;
        this.masterFeaturesCode = masterFeaturesCode;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Permissions create(String name, String description, String masterFeaturesCode) {
        LocalDateTime now = LocalDateTime.now();
        return new Permissions(UUID.randomUUID().toString(), name, description, masterFeaturesCode, now, now);
    }

    public static Permissions restore(String id, String name, String description, String masterFeaturesCode,
                                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Permissions(id, name, description, masterFeaturesCode, createdAt, updatedAt);
    }

    public void update(String name, String description, String masterFeaturesCode) {
        this.name = requireText(name, "name");
        this.description = description;
        this.masterFeaturesCode = masterFeaturesCode;
        this.updatedAt = LocalDateTime.now();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new OrgValidationException(fieldName + " is required");
        }
        return value.trim();
    }
}
