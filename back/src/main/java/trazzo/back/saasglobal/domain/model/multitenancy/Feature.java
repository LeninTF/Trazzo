package trazzo.back.saasglobal.domain.model.multitenancy;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Feature {

    private Integer id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Feature(Integer id, String name, String description,
                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = requireText(name, "name");
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Feature create(String name, String description) {
        LocalDateTime now = LocalDateTime.now(Clock.systemDefaultZone());
        return new Feature(null, name, description, now, now);
    }

    public static Feature restore(Integer id, String name, String description,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Feature(id, name, description, createdAt, updatedAt);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
