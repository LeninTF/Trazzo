package trazzo.back.saasglobal.domain.model.request;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class RequestRecordTest {

    @Test
    void create_setsFieldsCorrectly() {
        var before = LocalDateTime.now();
        RequestRecord rr = RequestRecord.create(10, "APPROVED", "user-1", "Cumple requisitos");
        var after = LocalDateTime.now();

        assertNull(rr.getId());
        assertEquals(10, rr.getRequestId());
        assertEquals("APPROVED", rr.getStatus());
        assertEquals("user-1", rr.getUserId());
        assertEquals("Cumple requisitos", rr.getChangeReason());
        assertFalse(rr.getCreatedAt().isBefore(before));
        assertFalse(rr.getCreatedAt().isAfter(after));
    }

    @Test
    void restore_setsAllFields() {
        var now = LocalDateTime.now();
        RequestRecord rr = RequestRecord.restore(3, 10, "REJECTED", "user-2", "No aplica", now);

        assertEquals(3, rr.getId());
        assertEquals("REJECTED", rr.getStatus());
        assertEquals("user-2", rr.getUserId());
        assertEquals(now, rr.getCreatedAt());
    }

    @Test
    void create_throwsWhenRequestIdNull() {
        assertThrows(IllegalArgumentException.class, () ->
                RequestRecord.create(null, "APPROVED", "user-1", "razon"));
    }

    @Test
    void create_throwsWhenStatusBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                RequestRecord.create(10, " ", "user-1", "razon"));
    }

    @Test
    void create_throwsWhenStatusNull() {
        assertThrows(IllegalArgumentException.class, () ->
                RequestRecord.create(10, null, "user-1", "razon"));
    }
}
