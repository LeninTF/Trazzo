package trazzo.back.incidents.domain.model;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IncidentType {

    private String id;
    private String nombre;
    private String descripcion;
    private boolean activo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    transient Clock clock = Clock.systemDefaultZone();

    private IncidentType(
            String id,
            String nombre,
            String descripcion,
            boolean activo,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = normalizeOptionalId(id);
        this.nombre = requireText(nombre, "nombre");
        this.descripcion = normalizeOptionalText(descripcion);
        this.activo = activo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static IncidentType create(String nombre, String descripcion) {
        LocalDateTime now = LocalDateTime.now();
        return new IncidentType(null, nombre, descripcion, true, now, now);
    }

    public static IncidentType restore(
            String id,
            String nombre,
            String descripcion,
            boolean activo,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new IncidentType(id, nombre, descripcion, activo, createdAt, updatedAt);
    }

    public void rename(String nombre) {
        this.nombre = requireText(nombre, "nombre");
        touch();
    }

    public void updateDescription(String descripcion) {
        this.descripcion = normalizeOptionalText(descripcion);
        touch();
    }

    public void activate() {
        this.activo = true;
        touch();
    }

    public void deactivate() {
        this.activo = false;
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now(clock);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptionalId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
