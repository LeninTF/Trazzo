package trazzo.back.corehr.domain.model.schedule;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.corehr.domain.exception.InvalidToleranciaException;
import trazzo.back.corehr.domain.model.ToleranciaType;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tolerancia {

    private Long id;
    private Long scheduleId;
    private String name;
    private ToleranciaType type;
    private Integer minutes;
    private String description;
    private boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    transient Clock clock = Clock.systemDefaultZone();

    private Tolerancia(
            Long id,
            Long scheduleId,
            String name,
            ToleranciaType type,
            Integer minutes,
            String description,
            boolean activo,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.scheduleId = scheduleId;
        this.name = normalizeOptionalText(name);
        this.type = requireType(type);
        this.minutes = requirePositiveMinutes(minutes);
        this.description = normalizeOptionalText(description);
        this.activo = activo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Tolerancia create(Long scheduleId, String name, ToleranciaType type, Integer minutes, String description) {
        LocalDateTime now = LocalDateTime.now();
        return new Tolerancia(null, scheduleId, name, type, minutes, description, true, now, now);
    }

    public static Tolerancia restore(
            Long id,
            Long scheduleId,
            String name,
            ToleranciaType type,
            Integer minutes,
            String description,
            boolean activo,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new Tolerancia(id, scheduleId, name, type, minutes, description, activo, createdAt, updatedAt);
    }

    public void activate() {
        this.activo = true;
        touch();
    }

    public void deactivate() {
        this.activo = false;
        touch();
    }

    public void updateMinutes(Integer minutes) {
        this.minutes = requirePositiveMinutes(minutes);
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now(clock);
    }

    private static ToleranciaType requireType(ToleranciaType type) {
        if (type == null) {
            throw new InvalidToleranciaException("type is required");
        }
        return type;
    }

    private static Integer requirePositiveMinutes(Integer minutes) {
        if (minutes == null || minutes < 0) {
            throw new InvalidToleranciaException("minutes must be a non-negative integer");
        }
        return minutes;
    }

    private static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
