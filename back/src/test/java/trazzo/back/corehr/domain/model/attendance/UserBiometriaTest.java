package trazzo.back.corehr.domain.model.attendance;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.exception.CoreHrValidationException;
import java.time.LocalDateTime;

class UserBiometriaTest {

    private static final String TEMPLATE = "YmFzZTY0dGVtcGxhdGU=";
    private static final String AES_KEY = "YmFzZTY0YWVzS2V5";
    private static final String IV = "YmFzZTY0aXY=";
    private static final String TAG = "YmFzZTY0dGFn";

    @Test
    void shouldCreateUserBiometria() {
        var ub = UserBiometria.create(1L, 1L, "DVC-001", 1, TEMPLATE, AES_KEY, IV, TAG, LocalDateTime.now(), null);
        assertNull(ub.getId());
        assertEquals(1L, ub.getTenantUserId());
        assertEquals(1, ub.getFingerIndex());
        assertTrue(ub.isActivo());
        assertEquals(TEMPLATE, ub.getEncryptedTemplateBase64());
        assertEquals(AES_KEY, ub.getEncryptedAesKeyBase64());
    }

    @Test
    void shouldRestoreUserBiometria() {
        var now = java.time.LocalDateTime.now();
        var ub = UserBiometria.restore(1L, 1L, 1L, "DVC-001", 1, TEMPLATE, AES_KEY, IV, TAG, now, true, now, now);
        assertEquals(1L, ub.getId());
        assertTrue(ub.isActivo());
    }

    @Test
    void shouldActivateAndDeactivate() {
        var ub = UserBiometria.create(1L, 1L, "DVC-001", 1, TEMPLATE, AES_KEY, IV, TAG, LocalDateTime.now(), null);
        ub.deactivate();
        assertFalse(ub.isActivo());
        ub.activate();
        assertTrue(ub.isActivo());
    }

    @Test
    void shouldThrowWhenTenantUserIdIsNull() {
        assertThrows(CoreHrValidationException.class, () ->
            UserBiometria.create(null, 1L, "DVC-001", 1, TEMPLATE, AES_KEY, IV, TAG, LocalDateTime.now(), null)
        );
    }

    @Test
    void aSealedRowNeedsNoWrappedKey() {
        var ub = UserBiometria.create(1L, 1L, "DVC-001", 1, TEMPLATE, null, IV, TAG, LocalDateTime.now(), "storage-v1");

        assertEquals("storage-v1", ub.getKeyRef());
        assertNull(ub.getEncryptedAesKeyBase64());
    }

    @Test
    void aSealedRowWithoutIvOrTagIsRejected() {
        assertThrows(CoreHrValidationException.class, () ->
            UserBiometria.create(1L, 1L, "DVC-001", 1, TEMPLATE, null, null, TAG, LocalDateTime.now(), "storage-v1"));
        assertThrows(CoreHrValidationException.class, () ->
            UserBiometria.create(1L, 1L, "DVC-001", 1, TEMPLATE, null, IV, " ", LocalDateTime.now(), "storage-v1"));
    }

    @Test
    void aLegacyTransportRowStillNeedsItsWrappedKey() {
        assertThrows(CoreHrValidationException.class, () ->
            UserBiometria.create(1L, 1L, "DVC-001", 1, TEMPLATE, null, IV, TAG, LocalDateTime.now(), null));
    }

    @Test
    void restoringARowFromBeforeKeyRefLeavesItNull() {
        var now = LocalDateTime.now();
        var ub = UserBiometria.restore(1L, 1L, 1L, "DVC-001", 1, TEMPLATE, AES_KEY, IV, TAG, now, true, now, now);

        assertNull(ub.getKeyRef());
    }
}
