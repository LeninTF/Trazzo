package trazzo.back.corehr.domain.specification;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.domain.model.ToleranciaType;
import trazzo.back.corehr.domain.model.schedule.Tolerancia;
import java.util.List;

class ToleranciaUniquenessSpecTest {

    private final ToleranciaUniquenessSpec spec = new ToleranciaUniquenessSpec();

    @Test
    void shouldAllowWhenNoActiveOfType() {
        var t1 = Tolerancia.restore(1L, 1L, "T1", ToleranciaType.ENTRADA, 10, null, false, null, null);
        assertTrue(spec.hasUniqueActiveType(List.of(t1), ToleranciaType.ENTRADA));
    }

    @Test
    void shouldAllowWhenOneActiveOfType() {
        var t1 = Tolerancia.restore(1L, 1L, "T1", ToleranciaType.ENTRADA, 10, null, true, null, null);
        assertTrue(spec.hasUniqueActiveType(List.of(t1), ToleranciaType.ENTRADA));
    }

    @Test
    void shouldRejectWhenMultipleActiveOfType() {
        var t1 = Tolerancia.restore(1L, 1L, "T1", ToleranciaType.ENTRADA, 10, null, true, null, null);
        var t2 = Tolerancia.restore(2L, 1L, "T2", ToleranciaType.ENTRADA, 15, null, true, null, null);
        assertFalse(spec.hasUniqueActiveType(List.of(t1, t2), ToleranciaType.ENTRADA));
    }

    @Test
    void shouldReturnTrueWhenToleranciasIsNull() {
        assertTrue(spec.hasUniqueActiveType(null, ToleranciaType.ENTRADA));
    }

    @Test
    void shouldReturnTrueWhenTypeIsNull() {
        var t1 = Tolerancia.restore(1L, 1L, "T1", ToleranciaType.ENTRADA, 10, null, true, null, null);
        assertTrue(spec.hasUniqueActiveType(List.of(t1), null));
    }
}
