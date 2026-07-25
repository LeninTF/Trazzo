package trazzo.back.audit.domain.model.master;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Map;

class AuditTest {

    @Test
    void shouldRestoreAudit() {
        var now = LocalDateTime.now();
        var previous = Map.<String, Object>of("name", "old");
        var value = Map.<String, Object>of("name", "new");
        var a = Audit.restore("1", "User", "42", Action.UPDATE, "user-1",
                "/api/users/42", "192.168.1.1", "Mozilla/5.0",
                previous, value, now);
        assertEquals("1", a.getId());
        assertEquals("User", a.getEntity());
        assertEquals("42", a.getEntityId());
        assertEquals(Action.UPDATE, a.getAction());
        assertEquals("user-1", a.getUserId());
        assertEquals("/api/users/42", a.getEndpoint());
        assertEquals("192.168.1.1", a.getIpAddress());
        assertEquals("Mozilla/5.0", a.getUserAgent());
        assertEquals(previous, a.getPreviousValue());
        assertEquals(value, a.getNewValue());
        assertEquals(now, a.getCreatedAt());
    }

}
