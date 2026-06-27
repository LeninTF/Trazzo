package trazzo.back.corehr.domain.model.attendance;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.exception.CoreHrValidationException;
import java.time.LocalDateTime;

class UserBiometriaTest {

    @Test
    void shouldCreateUserBiometria() {
        var ub = UserBiometria.create(1L, 1L, 1, LocalDateTime.now());
        assertNull(ub.getId());
        assertEquals(1L, ub.getTenantUserId());
        assertEquals(1, ub.getFingerIndex());
        assertTrue(ub.isActivo());
    }

    @Test
    void shouldRestoreUserBiometria() {
        var now = java.time.LocalDateTime.now();
        var ub = UserBiometria.restore(1L, 1L, 1L, 1, now, true, now, now);
        assertEquals(1L, ub.getId());
        assertTrue(ub.isActivo());
    }

    @Test
    void shouldActivateAndDeactivate() {
        var ub = UserBiometria.create(1L, 1L, 1, LocalDateTime.now());
        ub.deactivate();
        assertFalse(ub.isActivo());
        ub.activate();
        assertTrue(ub.isActivo());
    }

    @Test
    void shouldThrowWhenTenantUserIdIsNull() {
        assertThrows(CoreHrValidationException.class, () ->
            UserBiometria.create(null, 1L, 1, LocalDateTime.now())
        );
    }
}
