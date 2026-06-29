package trazzo.back.corehr.domain.model.attendance;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.corehr.domain.exception.CoreHrValidationException;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Device {

    private Long id;
    private String code;
    private String name;
    private String ip;
    private Integer puerto;
    private String ubicacion;
    private Long branchId;
    private boolean state;
    private LocalDateTime createdAt;

    private Device(
            Long id,
            String code,
            String name,
            String ip,
            Integer puerto,
            String ubicacion,
            Long branchId,
            boolean state,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.code = requireText(code, "code");
        this.name = normalizeOptionalText(name);
        this.ip = normalizeOptionalText(ip);
        this.puerto = puerto;
        this.ubicacion = normalizeOptionalText(ubicacion);
        this.branchId = branchId;
        this.state = state;
        this.createdAt = createdAt;
    }

    public static Device create(String code, String name, String ip, Integer puerto, String ubicacion, Long branchId) {
        return new Device(null, code, name, ip, puerto, ubicacion, branchId, true, LocalDateTime.now());
    }

    public static Device restore(
            Long id,
            String code,
            String name,
            String ip,
            Integer puerto,
            String ubicacion,
            Long branchId,
            boolean state,
            LocalDateTime createdAt
    ) {
        return new Device(id, code, name, ip, puerto, ubicacion, branchId, state, createdAt);
    }

    public void activate() {
        this.state = true;
    }

    public void deactivate() {
        this.state = false;
    }

    public void updateLocation(String ip, Integer puerto, String ubicacion) {
        this.ip = normalizeOptionalText(ip);
        this.puerto = puerto;
        this.ubicacion = normalizeOptionalText(ubicacion);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new CoreHrValidationException(fieldName + " is required");
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
