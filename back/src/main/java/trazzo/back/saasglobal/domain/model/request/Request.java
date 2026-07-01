package trazzo.back.saasglobal.domain.model.request;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Request {

    public enum Type   { TRIAL, INFO }
    public enum Status { PENDING, IN_REVIEW, APPROVED, REJECTED }

    private Integer id;
    private Type type;
    private String title;
    private String message;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Request(Integer id, Type type, String title, String message, Status status,
                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.type = requireNonNull(type, "type");
        this.title = requireText(title, "title");
        this.message = requireText(message, "message");
        this.status = status != null ? status : Status.PENDING;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Request create(Type type, String title, String message) {
        LocalDateTime now = LocalDateTime.now(Clock.systemDefaultZone());
        return new Request(null, type, title, message, Status.PENDING, now, now);
    }

    @SuppressWarnings("java:S107")
    public static Request restore(Integer id, Type type, String title, String message,
                                  Status status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new Request(id, type, title, message, status, createdAt, updatedAt);
    }

    public void transition(Status newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now(Clock.systemDefaultZone());
    }

    private static String requireText(String v, String fieldName) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(fieldName + " is required");
        return v.trim();
    }

    private static <T> T requireNonNull(T v, String fieldName) {
        if (v == null) throw new IllegalArgumentException(fieldName + " is required");
        return v;
    }
}
