package trazzo.back.incidents.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class IncidentTypeTest {

    /* == CREATION TESTS == */

    @Test
    void createWithNombreAndDescripcion() {
        var before = LocalDateTime.now();
        var type = IncidentType.create("Urgente", "Incidencias urgentes");
        var after = LocalDateTime.now();

        assertNull(type.getId());
        assertEquals("Urgente", type.getNombre());
        assertEquals("Incidencias urgentes", type.getDescripcion());
        assertTrue(type.isActivo());
        assertNotNull(type.getCreatedAt());
        assertNotNull(type.getUpdatedAt());
        assertFalse(type.getCreatedAt().isBefore(before));
        assertFalse(type.getCreatedAt().isAfter(after));
        assertFalse(type.getUpdatedAt().isBefore(before));
        assertFalse(type.getUpdatedAt().isAfter(after));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void createWithBlankNombreThrowsException(String nombre) {
        assertThrows(
                IllegalArgumentException.class,
                () -> IncidentType.create(nombre, "descripcion")
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void createWithBlankDescripcionNormalizesToNull(String descripcion) {
        var type = IncidentType.create("Urgente", descripcion);
        assertNull(type.getDescripcion());
    }

    @Test
    void createInitialState() {
        var type = IncidentType.create("Urgente", "Desc");
        assertNull(type.getId());
        assertTrue(type.isActivo());
        assertNotNull(type.getCreatedAt());
        assertNotNull(type.getUpdatedAt());
        assertEquals(type.getCreatedAt(), type.getUpdatedAt());
    }

    /* == RESTORATION TESTS == */

    @Test
    void restoreWithAllFields() {
        var now = LocalDateTime.now();
        var type = IncidentType.restore(
                "id-1", "Normal", "Incidencias normales",
                false, now, now
        );

        assertEquals("id-1", type.getId());
        assertEquals("Normal", type.getNombre());
        assertEquals("Incidencias normales", type.getDescripcion());
        assertFalse(type.isActivo());
        assertEquals(now, type.getCreatedAt());
        assertEquals(now, type.getUpdatedAt());
    }

    @Test
    void restoreWithBlankIdNormalizesToNull() {
        var now = LocalDateTime.now();
        var type = IncidentType.restore(
                " ", "Normal", null, true, now, now
        );
        assertNull(type.getId());
    }

    @Test
    void restoreWithBlankNombreThrowsException() {
        var now = LocalDateTime.now();
        assertThrows(
                IllegalArgumentException.class,
                () -> IncidentType.restore("id-1", " ", "desc", true, now, now)
        );
    }

    @Test
    void restoreWithBlankDescripcionNormalizesToNull() {
        var now = LocalDateTime.now();
        var type = IncidentType.restore(
                "id-1", "Normal", " ", true, now, now
        );
        assertNull(type.getDescripcion());
    }

    /* == RENAME TESTS == */

    @Test
    void renameSuccessfully() {
        var type = IncidentType.create("Original", "Desc");
        type.rename("Modificado");

        assertEquals("Modificado", type.getNombre());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void renameWithBlankThrowsException(String nombre) {
        var type = IncidentType.create("Original", "Desc");
        assertThrows(
                IllegalArgumentException.class,
                () -> type.rename(nombre)
        );
        assertEquals("Original", type.getNombre());
    }

    @Test
    void renameUpdatesUpdatedAt() {
        var type = IncidentType.create("Original", "Desc");
        var originalUpdatedAt = type.getUpdatedAt();

        sleep();
        type.rename("Modificado");

        assertTrue(type.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    /* == UPDATE DESCRIPTION TESTS == */

    @Test
    void updateDescriptionSuccessfully() {
        var type = IncidentType.create("Tipo", "Original");
        type.updateDescription("Modificada");

        assertEquals("Modificada", type.getDescripcion());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void updateDescriptionWithBlankNormalizesToNull(String descripcion) {
        var type = IncidentType.create("Tipo", "Original");
        type.updateDescription(descripcion);

        assertNull(type.getDescripcion());
    }

    @Test
    void updateDescriptionUpdatesUpdatedAt() {
        var type = IncidentType.create("Tipo", "Original");
        var originalUpdatedAt = type.getUpdatedAt();

        sleep();
        type.updateDescription("Nueva desc");

        assertTrue(type.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    /* == ACTIVATION / DEACTIVATION TESTS == */

    @Test
    void activateInactiveType() {
        var now = LocalDateTime.now();
        var type = IncidentType.restore("id-1", "Tipo", "Desc", false, now, now);

        assertFalse(type.isActivo());
        type.activate();

        assertTrue(type.isActivo());
    }

    @Test
    void deactivateActiveType() {
        var type = IncidentType.create("Tipo", "Desc");

        assertTrue(type.isActivo());
        type.deactivate();

        assertFalse(type.isActivo());
    }

    @Test
    void activationUpdatesUpdatedAt() {
        var now = LocalDateTime.now();
        var type = IncidentType.restore("id-1", "Tipo", "Desc", false, now, now);
        var originalUpdatedAt = type.getUpdatedAt();

        sleep();
        type.activate();

        assertTrue(type.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    @Test
    void deactivationUpdatesUpdatedAt() {
        var type = IncidentType.create("Tipo", "Desc");
        var originalUpdatedAt = type.getUpdatedAt();

        sleep();
        type.deactivate();

        assertTrue(type.getUpdatedAt().isAfter(originalUpdatedAt));
    }

    /* == VALIDATION TESTS == */

    @Test
    void createTrimsWhitespaceFromNombre() {
        var type = IncidentType.create("  Urgente  ", "Desc");
        assertEquals("Urgente", type.getNombre());
    }

    @Test
    void createTrimsWhitespaceFromDescripcion() {
        var type = IncidentType.create("Urgente", "  Desc  ");
        assertEquals("Desc", type.getDescripcion());
    }

    @Test
    void renameTrimsWhitespace() {
        var type = IncidentType.create("Original", "Desc");
        type.rename("  Nuevo  ");
        assertEquals("Nuevo", type.getNombre());
    }

    @Test
    void updateDescriptionTrimsWhitespace() {
        var type = IncidentType.create("Tipo", "Original");
        type.updateDescription("  Nueva  ");
        assertEquals("Nueva", type.getDescripcion());
    }

    @Test
    void restoreTrimsWhitespaceFromId() {
        var now = LocalDateTime.now();
        var type = IncidentType.restore("  id-1  ", "Normal", null, true, now, now);
        assertEquals("id-1", type.getId());
    }

    @Test
    void restoreTrimsWhitespaceFromNombre() {
        var now = LocalDateTime.now();
        var type = IncidentType.restore("id-1", "  Normal  ", null, true, now, now);
        assertEquals("Normal", type.getNombre());
    }

    /* == HELPER == */

    private static void sleep() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
