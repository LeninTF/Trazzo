package trazzo.back.corehr.domain.model.employee;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.exception.CoreHrValidationException;

class TenantContactTest {

    @Test
    void shouldCreateTenantContact() {
        var tc = TenantContact.create(1L, "EMAIL");
        assertNull(tc.getId());
        assertEquals(1L, tc.getTenantUserId());
        assertEquals("EMAIL", tc.getType());
        assertNull(tc.getDeletedAt());
    }

    @Test
    void shouldRestoreTenantContact() {
        var now = java.time.LocalDateTime.now();
        var tc = TenantContact.restore(1L, 1L, "PHONE", now, now, null);
        assertEquals(1L, tc.getId());
        assertEquals("PHONE", tc.getType());
    }

    @Test
    void shouldUpdateType() {
        var tc = TenantContact.create(1L, "EMAIL");
        tc.updateType("PHONE");
        assertEquals("PHONE", tc.getType());
    }

    @Test
    void shouldMarkAsDeleted() {
        var tc = TenantContact.create(1L, "EMAIL");
        tc.markAsDeleted();
        assertNotNull(tc.getDeletedAt());
    }

    @Test
    void shouldThrowWhenTenantUserIdIsNull() {
        assertThrows(CoreHrValidationException.class, () -> TenantContact.create(null, "EMAIL"));
    }

    @Test
    void shouldThrowWhenTypeIsNull() {
        assertThrows(CoreHrValidationException.class, () -> TenantContact.create(1L, null));
    }

    @Test
    void shouldThrowWhenTypeIsBlank() {
        assertThrows(CoreHrValidationException.class, () -> TenantContact.create(1L, "  "));
    }
}
