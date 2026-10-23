package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.corehr.application.dto.result.SyncItemResult;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public record MarcacionSyncResponse(
        String message,
        @JsonProperty("accepted_count") int acceptedCount,
        @JsonProperty("correlation_id") UUID correlationId,
        List<Item> items
) {
    /** status: accepted (registrado), skipped (ya estaba registrado) o failed (rechazado; message dice por qué). */
    public record Item(
            @JsonProperty("offline_event_id") Integer offlineEventId,
            String status,
            String message
    ) {
        public static Item from(SyncItemResult result) {
            return new Item(result.offlineEventId(), result.status().name().toLowerCase(Locale.ROOT), result.error());
        }
    }
}
