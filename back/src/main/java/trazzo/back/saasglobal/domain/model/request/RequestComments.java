package trazzo.back.saasglobal.domain.model.request;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RequestComments {

    private Integer id;
    private Integer requestId;
    private Integer requestContactId;
    private String comment;
    private LocalDateTime createdAt;

    private RequestComments(Integer id, Integer requestId, Integer requestContactId,
                            String comment, LocalDateTime createdAt) {
        this.id = id;
        this.requestId = requireNonNull(requestId, "requestId");
        this.requestContactId = requestContactId;
        this.comment = requireText(comment, "comment");
        this.createdAt = createdAt;
    }

    public static RequestComments create(Integer requestId, Integer requestContactId, String comment) {
        return new RequestComments(null, requestId, requestContactId, comment,
                LocalDateTime.now(Clock.systemDefaultZone()));
    }

    public static RequestComments restore(Integer id, Integer requestId, Integer requestContactId,
                                          String comment, LocalDateTime createdAt) {
        return new RequestComments(id, requestId, requestContactId, comment, createdAt);
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
