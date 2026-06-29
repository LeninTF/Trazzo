package trazzo.back.corehr.domain.model.schedule;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.exception.InvalidToleranciaException;
import trazzo.back.corehr.domain.model.ToleranciaType;

class ToleranciaTest {

    @Test
    void shouldCreateTolerancia() {
        var t = Tolerancia.create(1L, "Tol1", ToleranciaType.ENTRADA, 10, "Entry tolerance");
        assertNull(t.getId());
        assertEquals(1L, t.getScheduleId());
        assertTrue(t.isActivo());
        assertEquals(10, t.getMinutes());
    }

    @Test
    void shouldRestoreTolerancia() {
        var now = java.time.LocalDateTime.now();
        var t = Tolerancia.restore(1L, 1L, "Tol1", ToleranciaType.ENTRADA, 10, "Desc", true, now, now);
        assertEquals(1L, t.getId());
        assertTrue(t.isActivo());
    }

    @Test
    void shouldActivateAndDeactivate() {
        var t = Tolerancia.create(1L, "Tol1", ToleranciaType.SALIDA, 5, null);
        t.deactivate();
        assertFalse(t.isActivo());
        t.activate();
        assertTrue(t.isActivo());
    }

    @Test
    void shouldUpdateMinutes() {
        var t = Tolerancia.create(1L, "Tol1", ToleranciaType.ENTRADA, 10, null);
        t.updateMinutes(15);
        assertEquals(15, t.getMinutes());
    }

    @Test
    void shouldThrowWhenTypeIsNull() {
        assertThrows(InvalidToleranciaException.class, () ->
            Tolerancia.create(1L, "Tol1", null, 10, null)
        );
    }

    @Test
    void shouldThrowWhenMinutesNegative() {
        assertThrows(InvalidToleranciaException.class, () ->
            Tolerancia.create(1L, "Tol1", ToleranciaType.ENTRADA, -1, null)
        );
    }

    @Test
    void shouldThrowWhenMinutesNull() {
        assertThrows(InvalidToleranciaException.class, () ->
            Tolerancia.create(1L, "Tol1", ToleranciaType.ENTRADA, null, null)
        );
    }
}
