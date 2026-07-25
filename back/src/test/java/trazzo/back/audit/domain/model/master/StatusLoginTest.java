package trazzo.back.audit.domain.model.master;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class StatusLoginTest {

    @Test
    void shouldHaveSixValues() {
        assertEquals(6, StatusLogin.values().length);
    }

    @Test
    void shouldContainSucces() {
        assertEquals(StatusLogin.SUCCESS, StatusLogin.valueOf("SUCCESS"));
    }

    @Test
    void shouldContainFailedWrongPassword() {
        assertEquals(StatusLogin.FAILED_WRONG_PASSWORD, StatusLogin.valueOf("FAILED_WRONG_PASSWORD"));
    }

    @Test
    void shouldContainFailedUserNotFound() {
        assertEquals(StatusLogin.FAILED_USER_NOT_FOUND, StatusLogin.valueOf("FAILED_USER_NOT_FOUND"));
    }

    @Test
    void shouldContainFailedInactiveUser() {
        assertEquals(StatusLogin.FAILED_INACTIVE_USER, StatusLogin.valueOf("FAILED_INACTIVE_USER"));
    }

    @Test
    void shouldContainLockedOut() {
        assertEquals(StatusLogin.LOCKED_OUT, StatusLogin.valueOf("LOCKED_OUT"));
    }

    @Test
    void shouldContainLogoutExplicit() {
        assertEquals(StatusLogin.LOGOUT_EXPLICIT, StatusLogin.valueOf("LOGOUT_EXPLICIT"));
    }

}
