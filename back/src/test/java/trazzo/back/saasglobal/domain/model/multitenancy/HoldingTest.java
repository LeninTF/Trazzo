package trazzo.back.saasglobal.domain.model.multitenancy;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class HoldingTest {

    @Test
    void create_setsFieldsCorrectly() {
        var before = LocalDateTime.now();
        var h = Holding.create("20123456789", "Empresa SAC", HoldingType.PRIVATE);
        var after = LocalDateTime.now();

        assertNull(h.getId());
        assertEquals("20123456789", h.getTaxId());
        assertEquals("Empresa SAC", h.getLegalName());
        assertEquals(HoldingType.PRIVATE, h.getType());
        assertTrue(h.isActive());
        assertNull(h.getDeletedAt());
        assertFalse(h.getCreatedAt().isBefore(before));
        assertFalse(h.getCreatedAt().isAfter(after));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void create_throwsWhenTaxIdBlank(String taxId) {
        assertThrows(IllegalArgumentException.class,
                () -> Holding.create(taxId, "Empresa SAC", HoldingType.PRIVATE));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void create_throwsWhenLegalNameBlank(String legalName) {
        assertThrows(IllegalArgumentException.class,
                () -> Holding.create("20123456789", legalName, HoldingType.PRIVATE));
    }

    @Test
    void create_throwsWhenTypeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Holding.create("20123456789", "Empresa SAC", null));
    }

    @Test
    void restore_setsAllFields() {
        var now = LocalDateTime.now();
        var h = Holding.restore(3, "20999999999", "Corp SA", HoldingType.PUBLIC,
                false, now, now, now);

        assertEquals(3, h.getId());
        assertEquals("20999999999", h.getTaxId());
        assertEquals("Corp SA", h.getLegalName());
        assertEquals(HoldingType.PUBLIC, h.getType());
        assertFalse(h.isActive());
        assertEquals(now, h.getCreatedAt());
        assertEquals(now, h.getDeletedAt());
    }

    @Test
    void deactivate_setsActiveFalse() {
        var h = Holding.create("20123456789", "Empresa SAC", HoldingType.PRIVATE);
        assertTrue(h.isActive());
        h.deactivate();
        assertFalse(h.isActive());
    }

    @Test
    void activate_setsActiveTrue() {
        var now = LocalDateTime.now();
        var h = Holding.restore(1, "20123456789", "Empresa SAC", HoldingType.PRIVATE,
                false, now, now, null);
        assertFalse(h.isActive());
        h.activate();
        assertTrue(h.isActive());
    }
}
