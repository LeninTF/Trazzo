package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.UUID;

class MarcacionSyncResponseTest {

    @Test
    void shouldConstructRecordWithAllFields() {
        var correlationId = UUID.randomUUID();
        var response = new MarcacionSyncResponse("ok", 10, correlationId);
        assertEquals("ok", response.message());
        assertEquals(10, response.acceptedCount());
        assertEquals(correlationId, response.correlationId());
    }

    @Test
    void shouldHaveValueEquality() {
        var id = UUID.randomUUID();
        var a = new MarcacionSyncResponse("done", 5, id);
        var b = new MarcacionSyncResponse("done", 5, id);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenFieldsDiffer() {
        var id = UUID.randomUUID();
        var a = new MarcacionSyncResponse("ok", 5, id);
        var b = new MarcacionSyncResponse("fail", 5, id);
        assertNotEquals(a, b);
    }
}
