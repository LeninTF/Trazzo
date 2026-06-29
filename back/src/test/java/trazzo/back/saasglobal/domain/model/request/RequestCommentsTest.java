package trazzo.back.saasglobal.domain.model.request;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class RequestCommentsTest {

    @Test
    void create_setsFieldsCorrectly() {
        var before = LocalDateTime.now();
        RequestComments rc = RequestComments.create(10, 5, "Comentario de prueba");
        var after = LocalDateTime.now();

        assertNull(rc.getId());
        assertEquals(10, rc.getRequestId());
        assertEquals(5, rc.getRequestContactId());
        assertEquals("Comentario de prueba", rc.getComment());
        assertFalse(rc.getCreatedAt().isBefore(before));
        assertFalse(rc.getCreatedAt().isAfter(after));
    }

    @Test
    void restore_setsAllFields() {
        var now = LocalDateTime.now();
        RequestComments rc = RequestComments.restore(1, 10, 5, "Comentario", now);

        assertEquals(1, rc.getId());
        assertEquals(10, rc.getRequestId());
        assertEquals("Comentario", rc.getComment());
        assertEquals(now, rc.getCreatedAt());
    }

    @Test
    void create_throwsWhenRequestIdNull() {
        assertThrows(IllegalArgumentException.class, () ->
                RequestComments.create(null, 5, "Comentario"));
    }

    @Test
    void create_throwsWhenCommentBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                RequestComments.create(10, 5, " "));
    }

    @Test
    void create_throwsWhenCommentNull() {
        assertThrows(IllegalArgumentException.class, () ->
                RequestComments.create(10, 5, null));
    }
}
