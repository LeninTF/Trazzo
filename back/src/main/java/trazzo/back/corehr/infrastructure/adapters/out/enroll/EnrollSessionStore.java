package trazzo.back.corehr.infrastructure.adapters.out.enroll;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class EnrollSessionStore {

    private final ConcurrentHashMap<String, EnrollSession> sessions = new ConcurrentHashMap<>();

    public void createSession(EnrollSession session) {
        sessions.put(session.enrollToken(), session);
    }

    public EnrollSession findAndConsume(String enrollToken) {
        var session = sessions.remove(enrollToken);
        if (session == null) return null;
        if (LocalDateTime.now().isAfter(session.expiresAt())) return null;
        return session;
    }

    public boolean existsActiveSession(Long tenantUserId, Long deviceId) {
        return sessions.values().stream().anyMatch(s ->
                s.tenantUserId().equals(tenantUserId)
                        && s.deviceId().equals(deviceId)
                        && LocalDateTime.now().isBefore(s.expiresAt())
        );
    }
}
