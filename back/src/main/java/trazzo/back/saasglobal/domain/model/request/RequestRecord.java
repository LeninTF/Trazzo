package trazzo.back.saasglobal.domain.model.request;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RequestRecord {

    private Integer id;
    private Integer requestId;
    private String status;
    private String userId;
    private String changeReason;
    private LocalDateTime createdAt;

    private RequestRecord(Integer id, Integer requestId, String status, String userId,
                          String changeReason, LocalDateTime createdAt) {
        this.id = id;
        this.requestId = requireNonNull(requestId, "requestId");
        this.status = requireText(status, "status");
        this.userId = userId;
        this.changeReason = changeReason;
        this.createdAt = createdAt;
    }

    public static RequestRecord create(Integer requestId, String status,
                                       String userId, String changeReason) {
        return new RequestRecord(null, requestId, status, userId, changeReason,
                LocalDateTime.now(Clock.systemDefaultZone()));
    }

    @SuppressWarnings("java:S107")
    public static RequestRecord restore(Integer id, Integer requestId, String status,
                                        String userId, String changeReason,
                                        LocalDateTime createdAt) {
        return new RequestRecord(id, requestId, status, userId, changeReason, createdAt);
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
