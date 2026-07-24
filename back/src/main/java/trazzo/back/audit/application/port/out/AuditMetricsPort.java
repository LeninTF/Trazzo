package trazzo.back.audit.application.port.out;

import java.time.LocalDateTime;

public interface AuditMetricsPort {
    long countAll();
    long countSince(LocalDateTime since);
    long countBetween(LocalDateTime from, LocalDateTime to);
    long countByAction(String action);
    long countActiveSessions();
}
