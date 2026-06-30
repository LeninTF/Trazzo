package trazzo.back.organization.domain.model.business;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.organization.domain.exception.OrgValidationException;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Department {

    private Long id;
    private Long areaId;
    private String name;
    private String description;
    private boolean state;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    private Department(Long id, Long areaId, String name, String description, boolean state,
                       LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        this.id = id;
        this.areaId = requireId(areaId, "areaId");
        this.name = requireText(name, "name");
        this.description = description;
        this.state = state;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static Department create(Long areaId, String name, String description) {
        LocalDateTime now = LocalDateTime.now();
        return new Department(null, areaId, name, description, true, now, now, null);
    }

    public static Department restore(Long id, Long areaId, String name, String description, boolean state,
                                      LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime deletedAt) {
        return new Department(id, areaId, name, description, state, createdAt, updatedAt, deletedAt);
    }

    public void update(String name, String description) {
        this.name = requireText(name, "name");
        this.description = description;
        touch();
    }

    public void softDelete() {
        this.state = false;
        this.deletedAt = LocalDateTime.now();
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    private static Long requireId(Long value, String fieldName) {
        if (value == null) {
            throw new OrgValidationException(fieldName + " is required");
        }
        return value;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new OrgValidationException(fieldName + " is required");
        }
        return value.trim();
    }
}
