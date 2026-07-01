package trazzo.back.saasglobal.domain.model.invoice;

import java.time.Clock;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentWebhooksLog {

    private String id;
    private String mpEventId;
    private String action;
    private String rawPayload;
    private boolean processed;
    private LocalDateTime receivedAt;

    private PaymentWebhooksLog(String id, String mpEventId, String action, String rawPayload,
                               boolean processed, LocalDateTime receivedAt) {
        this.id = requireText(id, "id");
        this.mpEventId = mpEventId;
        this.action = action;
        this.rawPayload = rawPayload;
        this.processed = processed;
        this.receivedAt = receivedAt;
    }

    public static PaymentWebhooksLog create(String id, String mpEventId, String action, String rawPayload) {
        return new PaymentWebhooksLog(id, mpEventId, action, rawPayload, false,
                LocalDateTime.now(Clock.systemDefaultZone()));
    }

    public static PaymentWebhooksLog restore(String id, String mpEventId, String action,
                                             String rawPayload, boolean processed,
                                             LocalDateTime receivedAt) {
        return new PaymentWebhooksLog(id, mpEventId, action, rawPayload, processed, receivedAt);
    }

    public void markProcessed() {
        this.processed = true;
    }

    private static String requireText(String v, String fieldName) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(fieldName + " is required");
        return v.trim();
    }
}
