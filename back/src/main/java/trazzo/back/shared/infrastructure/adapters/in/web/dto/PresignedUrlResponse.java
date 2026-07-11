package trazzo.back.shared.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PresignedUrlResponse(
        @JsonProperty("presigned_url") String presignedUrl,
        @JsonProperty("object_key") String objectKey
) {}
