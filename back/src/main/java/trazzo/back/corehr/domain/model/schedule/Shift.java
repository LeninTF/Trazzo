package trazzo.back.corehr.domain.model.schedule;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.corehr.domain.exception.InvalidShiftException;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shift {

    private Long id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    transient Clock clock = Clock.systemDefaultZone();

    private Shift(Long id, String name, String description, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = requireText(name, "name");
        this.description = normalizeOptionalText(description);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Shift create(String name, String description) {
        LocalDateTime now = LocalDateTime.now();
        return new Shift(null, name, description, now, now);
    }

    public static Shift restore(Long id, String name, String description, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Shift(id, name, description, createdAt, updatedAt);
    }

    public void rename(String name) {
        this.name = requireText(name, "name");
        touch();
    }

    public void updateDescription(String description) {
        this.description = normalizeOptionalText(description);
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now(clock);
    }

    static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidShiftException(fieldName + " is required");
        }
        return value.trim();
    }

    static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
