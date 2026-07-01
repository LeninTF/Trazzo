package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import trazzo.back.incidents.application.dto.result.IncidentTypeResult;

import java.time.LocalDateTime;

class IncidentTypeResponseTest {

    @Test
    void fromResultMapsAllFields() {
        var now = LocalDateTime.now();
        var result = new IncidentTypeResult("id-1", "Permiso", "Desc", true, now, now);
        var response = IncidentTypeResponse.from(result);

        assertEquals("id-1", response.id());
        assertEquals("Permiso", response.nombre());
        assertEquals("Desc", response.descripcion());
        assertTrue(response.activo());
        assertEquals(now, response.createdAt());
        assertEquals(now, response.updatedAt());
    }

    @Test
    void fromResultWithNullDescripcion() {
        var now = LocalDateTime.now();
        var result = new IncidentTypeResult("id-1", "Permiso", null, false, now, now);
        var response = IncidentTypeResponse.from(result);

        assertNull(response.descripcion());
        assertFalse(response.activo());
    }
}
