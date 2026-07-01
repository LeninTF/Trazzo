package trazzo.back.corehr.domain.model.employee;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.exception.CoreHrValidationException;
import java.time.LocalDate;

class TenantUserDepartmentTest {

    @Test
    void shouldCreateTenantUserDepartment() {
        var tud = TenantUserDepartment.create(1L, 1L, true, LocalDate.of(2025, 1, 1), null);
        assertNull(tud.getId());
        assertTrue(tud.isPrimary());
        assertEquals(LocalDate.of(2025, 1, 1), tud.getStartDate());
        assertNull(tud.getEndDate());
    }

    @Test
    void shouldRestoreTenantUserDepartment() {
        var now = java.time.LocalDateTime.now();
        var tud = TenantUserDepartment.restore(1L, 1L, 1L, false, LocalDate.of(2025, 1, 1), null, now, now);
        assertEquals(1L, tud.getId());
        assertFalse(tud.isPrimary());
    }

    @Test
    void shouldMarkAsPrimary() {
        var tud = TenantUserDepartment.create(1L, 1L, false, LocalDate.of(2025, 1, 1), null);
        tud.markAsPrimary();
        assertTrue(tud.isPrimary());
        tud.unmarkAsPrimary();
        assertFalse(tud.isPrimary());
    }

    @Test
    void shouldEndAssignment() {
        var tud = TenantUserDepartment.create(1L, 1L, false, LocalDate.of(2025, 1, 1), null);
        tud.endAssignment(LocalDate.of(2025, 12, 31));
        assertEquals(LocalDate.of(2025, 12, 31), tud.getEndDate());
    }

    @Test
    void shouldThrowWhenTenantUserIdIsNull() {
        assertThrows(CoreHrValidationException.class, () ->
            TenantUserDepartment.create(null, 1L, false, LocalDate.of(2025, 1, 1), null)
        );
    }

    @Test
    void shouldThrowWhenDepartmentIdIsNull() {
        assertThrows(CoreHrValidationException.class, () ->
            TenantUserDepartment.create(1L, null, false, LocalDate.of(2025, 1, 1), null)
        );
    }

    @Test
    void shouldThrowWhenStartDateIsNull() {
        assertThrows(CoreHrValidationException.class, () ->
            TenantUserDepartment.create(1L, 1L, false, null, null)
        );
    }
}
