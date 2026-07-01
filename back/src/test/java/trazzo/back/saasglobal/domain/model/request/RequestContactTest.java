package trazzo.back.saasglobal.domain.model.request;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class RequestContactTest {

    @Test
    void create_setsFieldsCorrectly() {
        var before = LocalDateTime.now();
        RequestContact rc = RequestContact.create(1, "Juan", "Perez",
                "juan@example.com", "987654321", "20111111111", "Empresa SAC");
        var after = LocalDateTime.now();

        assertEquals(1, rc.getRequestId());
        assertEquals("Juan", rc.getName());
        assertEquals("Perez", rc.getLastName());
        assertEquals("juan@example.com", rc.getEmail());
        assertEquals("987654321", rc.getPhoneNumber());
        assertEquals("20111111111", rc.getTaxId());
        assertEquals("Empresa SAC", rc.getCompanyName());
        assertFalse(rc.getCreatedAt().isBefore(before));
        assertFalse(rc.getCreatedAt().isAfter(after));
    }

    @Test
    void restore_setsAllFields() {
        var now = LocalDateTime.now();
        RequestContact rc = RequestContact.restore(1, "Ana", "Lopez",
                "ana@example.com", "999111222", "20222222222", "Corp SA", now, now);

        assertEquals(1, rc.getRequestId());
        assertEquals("Ana", rc.getName());
        assertEquals(now, rc.getCreatedAt());
    }

    @Test
    void create_throwsWhenRequestIdNull() {
        assertThrows(IllegalArgumentException.class, () ->
                RequestContact.create(null, "Juan", "Perez",
                        "juan@example.com", "987654321", "20111111111", "Empresa SAC"));
    }

    @Test
    void create_throwsWhenNameBlank() {
        assertThrows(IllegalArgumentException.class, () ->
                RequestContact.create(1, " ", "Perez",
                        "juan@example.com", "987654321", "20111111111", "Empresa SAC"));
    }

    @Test
    void create_throwsWhenEmailNull() {
        assertThrows(IllegalArgumentException.class, () ->
                RequestContact.create(1, "Juan", "Perez",
                        null, "987654321", "20111111111", "Empresa SAC"));
    }
}
