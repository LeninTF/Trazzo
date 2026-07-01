package trazzo.back.incidents.infrastructure.adapters.in.web.dto;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PatchIncidentRequestTest {

    @Test
    void createWithComment() {
        var request = new PatchIncidentRequest("nuevo comentario");
        assertEquals("nuevo comentario", request.comment());
    }

    @Test
    void createWithNullComment() {
        var request = new PatchIncidentRequest(null);
        assertNull(request.comment());
    }
}
