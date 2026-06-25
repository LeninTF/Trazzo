package trazzo.back.saasglobal.domain.model.iam;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.saasglobal.domain.exception.UserValidationException;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MetodoRecuperacion {

    public enum Type { EMAIL, PHONE }

    private Integer id;
    private String usersId;
    private Type methodType;
    private String value;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @SuppressWarnings("java:S107")
    private MetodoRecuperacion(Integer id, String usersId, Type methodType, String value,
                               LocalDateTime createdAt, LocalDateTime updatedAt,
                               LocalDateTime deletedAt) {
        this.id = id;
        this.usersId = requireText(usersId, "usersId");
        this.methodType = requireNonNull(methodType, "methodType");
        this.value = requireText(value, "value");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static MetodoRecuperacion create(String usersId, Type methodType, String value) {
        LocalDateTime now = LocalDateTime.now(Clock.systemDefaultZone());
        return new MetodoRecuperacion(null, usersId, methodType, value, now, now, null);
    }

    @SuppressWarnings("java:S107")
    public static MetodoRecuperacion restore(Integer id, String usersId, Type methodType,
                                             String value, LocalDateTime createdAt,
                                             LocalDateTime updatedAt, LocalDateTime deletedAt) {
        return new MetodoRecuperacion(id, usersId, methodType, value, createdAt, updatedAt, deletedAt);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new UserValidationException(fieldName + " is required");
        }
        return value.trim();
    }

    private static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new UserValidationException(fieldName + " is required");
        }
        return value;
    }
}
