package trazzo.back.organization.domain.model.roles;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.organization.domain.OrgValidation;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Role {

    private String id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Role(String id, String name, String description,
                 LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = OrgValidation.requireText(name, "name");
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Role create(String name, String description) {
        LocalDateTime now = LocalDateTime.now();
        return new Role(UUID.randomUUID().toString(), name, description, now, now);
    }

    public static Role restore(String id, String name, String description,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Role(id, name, description, createdAt, updatedAt);
    }

    public void update(String name, String description) {
        this.name = OrgValidation.requireText(name, "name");
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

}
