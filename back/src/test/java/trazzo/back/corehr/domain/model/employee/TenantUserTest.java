package trazzo.back.corehr.domain.model.employee;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.exception.InvalidTenantUserException;
import trazzo.back.corehr.domain.model.TenantUserState;
import java.util.UUID;

class TenantUserTest {

    private final UUID masterUserId = UUID.randomUUID();

    @Test
    void shouldCreateTenantUser() {
        var tu = TenantUser.create(masterUserId);
        assertNull(tu.getId());
        assertEquals(masterUserId, tu.getMasterUserId());
        assertEquals(TenantUserState.ACTIVO, tu.getState());
        assertTrue(tu.isActive());
        assertNull(tu.getDeletedAt());
    }

    @Test
    void shouldRestoreTenantUser() {
        var now = java.time.LocalDateTime.now();
        var tu = TenantUser.restore(1L, masterUserId, TenantUserState.LICENCIA, now, now, null);
        assertEquals(1L, tu.getId());
        assertEquals(TenantUserState.LICENCIA, tu.getState());
    }

    @Test
    void shouldActivate() {
        var tu = TenantUser.create(masterUserId);
        tu.setLicencia();
        assertEquals(TenantUserState.LICENCIA, tu.getState());
        assertFalse(tu.isActive());
        tu.activate();
        assertEquals(TenantUserState.ACTIVO, tu.getState());
        assertTrue(tu.isActive());
    }

    @Test
    void shouldSetLicencia() {
        var tu = TenantUser.create(masterUserId);
        tu.setLicencia();
        assertEquals(TenantUserState.LICENCIA, tu.getState());
    }

    @Test
    void shouldDeactivate() {
        var tu = TenantUser.create(masterUserId);
        tu.deactivate();
        assertEquals(TenantUserState.INACTIVO, tu.getState());
        assertFalse(tu.isActive());
        assertNotNull(tu.getDeletedAt());
    }

    @Test
    void shouldThrowWhenMasterUserIdIsNull() {
        assertThrows(InvalidTenantUserException.class, () -> TenantUser.create(null));
    }

    @Test
    void shouldRestoreWithInactivo() {
        var now = java.time.LocalDateTime.now();
        var tu = TenantUser.restore(1L, masterUserId, TenantUserState.INACTIVO, now, now, now);
        assertEquals(TenantUserState.INACTIVO, tu.getState());
        assertNotNull(tu.getDeletedAt());
    }
}
