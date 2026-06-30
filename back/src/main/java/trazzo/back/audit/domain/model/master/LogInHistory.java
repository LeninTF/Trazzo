package trazzo.back.audit.domain.model.master;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class LogInHistory {
    private Long id;
    private String userId;
    private String attemptedEmail;
    private StatusLogin status;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;

    public LogInHistory(
            Long id,
            String userId,
            String attemptedEmail,
            StatusLogin status,
            String ipAddress,
            String userAgent,
            LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.attemptedEmail = attemptedEmail;
        this.status = status;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
    }
}
