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
        var entity = new IncidentEntity(1, 1, 1, IncidentState.PENDIENTE,
                "comment", null, now, now, List.of(), null);

        assertEquals(1, entity.getId());
        assertEquals(1, entity.getTenantUserId());
        assertEquals(IncidentState.PENDIENTE, entity.getState());
        assertEquals("comment", entity.getComment());
        assertNull(entity.getRejectionReason());
        assertTrue(entity.getEvidences().isEmpty());
        assertNull(entity.getPermission());
    }

    @Test
    void settersWorkCorrectly() {
        var entity = new IncidentEntity();
        entity.setId(1);
        entity.setState(IncidentState.APROBADO);
        entity.setComment("test");

        assertEquals(1, entity.getId());
        assertEquals(IncidentState.APROBADO, entity.getState());
        assertEquals("test", entity.getComment());
    }
}
