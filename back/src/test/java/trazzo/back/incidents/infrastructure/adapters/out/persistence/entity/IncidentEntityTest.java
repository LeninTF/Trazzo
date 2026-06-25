package trazzo.back.incidents.infrastructure.adapters.out.persistence.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import trazzo.back.incidents.domain.model.IncidentState;

import java.time.LocalDateTime;
import java.util.List;

class IncidentEntityTest {

    @Test
    void createInstance() {
        var now = LocalDateTime.now();
        var entity = new IncidentEntity("inc-1", "u-1", "t-1", IncidentState.PENDIENTE,
                "comment", null, now, now, List.of(), null);

        assertEquals("inc-1", entity.getId());
        assertEquals("u-1", entity.getTenantUserId());
        assertEquals(IncidentState.PENDIENTE, entity.getState());
        assertEquals("comment", entity.getComment());
        assertNull(entity.getRejectionReason());
        assertTrue(entity.getEvidences().isEmpty());
        assertNull(entity.getPermission());
    }

    @Test
    void settersWorkCorrectly() {
        var entity = new IncidentEntity();
        entity.setId("inc-1");
        entity.setState(IncidentState.APROBADO);
        entity.setComment("test");

        assertEquals("inc-1", entity.getId());
        assertEquals(IncidentState.APROBADO, entity.getState());
        assertEquals("test", entity.getComment());
    }
}
