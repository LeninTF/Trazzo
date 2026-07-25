package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record MarcacionSyncResponse(
        String message,
        @JsonProperty("accepted_count") int acceptedCount,
        @JsonProperty("correlation_id") UUID correlationId
) {
}
