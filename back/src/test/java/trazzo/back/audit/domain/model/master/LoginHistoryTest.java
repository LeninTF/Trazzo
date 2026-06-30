package trazzo.back.audit.domain.model.master;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

class LoginHistoryTest {

    @Test
    void shouldCreateLoginHistory() {
        var now = LocalDateTime.now();
        var h = new LogInHistory(1L, "user-1", "test@example.com",
                StatusLogin.SUCCES, "192.168.1.1", "Chrome", now);
        assertEquals(1L, h.getId());
        assertEquals("user-1", h.getUserId());
        assertEquals("test@example.com", h.getAttemptedEmail());
        assertEquals(StatusLogin.SUCCES, h.getStatus());
        assertEquals("192.168.1.1", h.getIpAddress());
        assertEquals("Chrome", h.getUserAgent());
        assertEquals(now, h.getCreatedAt());
    }

}
