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
        var h = Holding.create("20123456789", "Empresa SAC", HoldingType.PRIVADO);
        var after = LocalDateTime.now();

        assertNull(h.getId());
        assertEquals("20123456789", h.getTaxId());
        assertEquals("Empresa SAC", h.getLegalName());
        assertEquals(HoldingType.PRIVADO, h.getType());
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
                () -> Holding.create(taxId, "Empresa SAC", HoldingType.PRIVADO));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void create_throwsWhenLegalNameBlank(String legalName) {
        assertThrows(IllegalArgumentException.class,
                () -> Holding.create("20123456789", legalName, HoldingType.PRIVADO));
    }

    @Test
    void create_throwsWhenTypeNull() {
        assertThrows(IllegalArgumentException.class,
                () -> Holding.create("20123456789", "Empresa SAC", null));
    }

    @Test
    void restore_setsAllFields() {
        var now = LocalDateTime.now();
        var h = Holding.restore(3, "20999999999", "Corp SA", HoldingType.PUBLICO,
                false, now, now, now);

        assertEquals(3, h.getId());
        assertEquals("20999999999", h.getTaxId());
        assertEquals("Corp SA", h.getLegalName());
        assertEquals(HoldingType.PUBLICO, h.getType());
        assertFalse(h.isActive());
        assertEquals(now, h.getCreatedAt());
        assertEquals(now, h.getDeletedAt());
    }

    @Test
    void deactivate_setsActiveFalse() {
        var h = Holding.create("20123456789", "Empresa SAC", HoldingType.PRIVADO);
        assertTrue(h.isActive());
        h.deactivate();
        assertFalse(h.isActive());
    }

    @Test
    void activate_setsActiveTrue() {
        var now = LocalDateTime.now();
        var h = Holding.restore(1, "20123456789", "Empresa SAC", HoldingType.PRIVADO,
                false, now, now, null);
        assertFalse(h.isActive());
        h.activate();
        assertTrue(h.isActive());
    }

    @Test
    void update_changesLegalNameAndType() {
        var h = Holding.create("20123456789", "Empresa SAC", HoldingType.PRIVADO);
        h.update("Nueva Corp", HoldingType.PUBLICO);
        assertEquals("Nueva Corp", h.getLegalName());
        assertEquals(HoldingType.PUBLICO, h.getType());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void update_throwsWhenLegalNameBlank(String legalName) {
        var h = Holding.create("20123456789", "Empresa SAC", HoldingType.PRIVADO);
        assertThrows(IllegalArgumentException.class, () -> h.update(legalName, HoldingType.PUBLICO));
    }

    @Test
    void update_throwsWhenTypeNull() {
        var h = Holding.create("20123456789", "Empresa SAC", HoldingType.PRIVADO);
        assertThrows(IllegalArgumentException.class, () -> h.update("Corp SA", null));
    }

    @Test
    void delete_setsActiveAndDeletedAt() {
        var h = Holding.create("20123456789", "Empresa SAC", HoldingType.PRIVADO);
        assertTrue(h.isActive());
        assertNull(h.getDeletedAt());
        h.delete();
        assertFalse(h.isActive());
        assertNotNull(h.getDeletedAt());
    }
}
