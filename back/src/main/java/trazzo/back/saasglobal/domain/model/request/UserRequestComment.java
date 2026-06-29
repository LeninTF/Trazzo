package trazzo.back.saasglobal.domain.model.request;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRequestComment {

    private Integer id;
    private String userId;
    private Integer requestCommentId;
    private LocalDateTime createdAt;

    private UserRequestComment(Integer id, String userId, Integer requestCommentId,
                               LocalDateTime createdAt) {
        this.id = id;
        this.userId = requireText(userId, "userId");
        this.requestCommentId = requireNonNull(requestCommentId, "requestCommentId");
        this.createdAt = createdAt;
    }

    public static UserRequestComment create(String userId, Integer requestCommentId) {
        return new UserRequestComment(null, userId, requestCommentId,
                LocalDateTime.now(Clock.systemDefaultZone()));
    }

    public static UserRequestComment restore(Integer id, String userId, Integer requestCommentId,
                                             LocalDateTime createdAt) {
        return new UserRequestComment(id, userId, requestCommentId, createdAt);
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
