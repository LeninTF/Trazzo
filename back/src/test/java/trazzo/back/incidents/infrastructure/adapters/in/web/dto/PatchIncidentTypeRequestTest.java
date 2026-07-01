package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PatchIncidentTypeRequestTest {

    @Test
    void createWithAllFields() {
        var request = new PatchIncidentTypeRequest("Nuevo", "Desc", true);
        assertEquals("Nuevo", request.nombre());
        assertEquals("Desc", request.descripcion());
        assertTrue(request.activo());
    }

    @Test
    void createWithNullFields() {
        var request = new PatchIncidentTypeRequest(null, null, null);
        assertNull(request.nombre());
        assertNull(request.descripcion());
        assertNull(request.activo());
    }

    @Test
    void activoJsonProperty() {
        var request = new PatchIncidentTypeRequest(null, null, false);
        assertFalse(request.activo());
    }
}
