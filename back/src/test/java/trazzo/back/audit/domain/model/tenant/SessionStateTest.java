package trazzo.back.audit.domain.model.tenant;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SessionStateTest {

    @Test
    void shouldHaveFiveValues() {
        assertEquals(5, SessionState.values().length);
    }

    @Test
    void shouldContainActive() {
        assertEquals(SessionState.ACTIVE, SessionState.valueOf("ACTIVE"));
    }

    @Test
    void shouldContainExpired() {
        assertEquals(SessionState.EXPIRED, SessionState.valueOf("EXPIRED"));
    }

    @Test
    void shouldContainLoggedOut() {
        assertEquals(SessionState.LOGGED_OUT, SessionState.valueOf("LOGGED_OUT"));
    }

    @Test
    void shouldContainRevoked() {
        assertEquals(SessionState.REVOKED, SessionState.valueOf("REVOKED"));
    }

    @Test
    void shouldContainBlocked() {
        assertEquals(SessionState.BLOCKED, SessionState.valueOf("BLOCKED"));
    }

}
