package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.result.SyncItemResult;
import trazzo.back.corehr.domain.model.AttendanceState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

class MarcacionSyncResponseTest {

    @Test
    void shouldConstructRecordWithAllFields() {
        var correlationId = UUID.randomUUID();
        var response = new MarcacionSyncResponse("ok", 10, correlationId, List.of());
        assertEquals("ok", response.message());
        assertEquals(10, response.acceptedCount());
        assertEquals(correlationId, response.correlationId());
    }

    @Test
    void shouldHaveValueEquality() {
        var id = UUID.randomUUID();
        var a = new MarcacionSyncResponse("done", 5, id, List.of());
        var b = new MarcacionSyncResponse("done", 5, id, List.of());
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenFieldsDiffer() {
        var id = UUID.randomUUID();
        var a = new MarcacionSyncResponse("ok", 5, id, List.of());
        var b = new MarcacionSyncResponse("fail", 5, id, List.of());
        assertNotEquals(a, b);
    }

    @Test
    void anItemReportsTheOfflineEventItsStatusAndWhyItFailed() {
        var failed = MarcacionSyncResponse.Item.from(SyncItemResult.failure("Huella no reconocida", 7));
        var accepted = MarcacionSyncResponse.Item.from(
                SyncItemResult.success("att-1", AttendanceState.PUNTUAL, 0, LocalDateTime.of(2026, 10, 23, 8, 0), 8));

        assertEquals(new MarcacionSyncResponse.Item(7, "failed", "Huella no reconocida"), failed);
        assertEquals(new MarcacionSyncResponse.Item(8, "accepted", null), accepted);
        assertEquals("skipped", MarcacionSyncResponse.Item.from(SyncItemResult.skipped(9)).status());
    }
}
