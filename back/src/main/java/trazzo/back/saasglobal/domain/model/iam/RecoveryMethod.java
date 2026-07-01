package trazzo.back.saasglobal.domain.model.iam;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import trazzo.back.saasglobal.domain.exception.UserValidationException;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecoveryMethod {

    public enum Type { EMAIL, PHONE }

    private Integer id;
    private String userId;
    private Type methodType;
    private String value;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @SuppressWarnings("java:S107")
    private RecoveryMethod(Integer id, String userId, Type methodType, String value,
                           LocalDateTime createdAt, LocalDateTime updatedAt,
                           LocalDateTime deletedAt) {
        this.id = id;
        this.userId = requireText(userId, "userId");
        this.methodType = requireNonNull(methodType, "methodType");
        this.value = requireText(value, "value");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static RecoveryMethod create(String userId, Type methodType, String value) {
        LocalDateTime now = LocalDateTime.now(Clock.systemDefaultZone());
        return new RecoveryMethod(null, userId, methodType, value, now, now, null);
    }

    @SuppressWarnings("java:S107")
    public static RecoveryMethod restore(Integer id, String userId, Type methodType, String value,
                                         LocalDateTime createdAt, LocalDateTime updatedAt,
                                         LocalDateTime deletedAt) {
        return new RecoveryMethod(id, userId, methodType, value, createdAt, updatedAt, deletedAt);
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
