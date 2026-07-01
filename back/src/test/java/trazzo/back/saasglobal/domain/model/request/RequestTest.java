package trazzo.back.saasglobal.domain.model.request;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class RequestTest {

    @Test
    void create_setsFieldsCorrectly() {
        var before = LocalDateTime.now();
        Request r = Request.create(Request.Type.TRIAL, "Prueba SaaS", "Quiero probar el sistema");
        var after = LocalDateTime.now();

        assertNull(r.getId());
        assertEquals(Request.Type.TRIAL, r.getType());
        assertEquals("Prueba SaaS", r.getTitle());
        assertEquals("Quiero probar el sistema", r.getMessage());
        assertEquals(Request.Status.PENDING, r.getStatus());
        assertFalse(r.getCreatedAt().isBefore(before));
        assertFalse(r.getCreatedAt().isAfter(after));
    }

    @Test
    void restore_setsAllFields() {
        var now = LocalDateTime.now();
        Request r = Request.restore(5, Request.Type.INFO, "Info", "Mensaje",
                Request.Status.APPROVED, now, now);

        assertEquals(5, r.getId());
        assertEquals(Request.Type.INFO, r.getType());
        assertEquals(Request.Status.APPROVED, r.getStatus());
        assertEquals(now, r.getCreatedAt());
    }

    @Test
    void restore_defaultsStatusToPendingWhenNull() {
        var now = LocalDateTime.now();
        Request r = Request.restore(1, Request.Type.TRIAL, "titulo", "mensaje", null, now, now);

        assertEquals(Request.Status.PENDING, r.getStatus());
    }

    @Test
    void transition_changesStatus() {
        Request r = Request.create(Request.Type.TRIAL, "titulo", "mensaje");
        assertEquals(Request.Status.PENDING, r.getStatus());

        r.transition(Request.Status.IN_REVIEW);
        assertEquals(Request.Status.IN_REVIEW, r.getStatus());

        r.transition(Request.Status.APPROVED);
        assertEquals(Request.Status.APPROVED, r.getStatus());
    }

    @Test
    void create_throwsWhenTypeNull() {
        assertThrows(IllegalArgumentException.class, () ->
                Request.create(null, "titulo", "mensaje"));
    }

    @Test
    void create_throwsWhenTitleBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                Request.create(Request.Type.TRIAL, "", "mensaje"));
    }

    @Test
    void create_throwsWhenMessageNull() {
        assertThrows(IllegalArgumentException.class, () ->
                Request.create(Request.Type.TRIAL, "titulo", null));
    }
}
