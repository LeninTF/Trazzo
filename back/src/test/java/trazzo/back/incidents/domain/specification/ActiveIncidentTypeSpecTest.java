package trazzo.back.incidents.domain.specification;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import trazzo.back.incidents.domain.model.IncidentType;

import java.time.LocalDateTime;

class ActiveIncidentTypeSpecTest {

    private final ActiveIncidentTypeSpec spec = new ActiveIncidentTypeSpec();

    @Test
    void isSatisfiedByActiveType() {
        var type = IncidentType.restore("id-1", "Permiso", "Desc", true, LocalDateTime.now(), LocalDateTime.now());
        assertTrue(spec.isSatisfiedBy(type));
    }

    @Test
    void isNotSatisfiedByInactiveType() {
        var type = IncidentType.restore("id-1", "Permiso", "Desc", false, LocalDateTime.now(), LocalDateTime.now());
        assertFalse(spec.isSatisfiedBy(type));
    }

    @Test
    void isNotSatisfiedByNullType() {
        assertFalse(spec.isSatisfiedBy(null));
    }
}
