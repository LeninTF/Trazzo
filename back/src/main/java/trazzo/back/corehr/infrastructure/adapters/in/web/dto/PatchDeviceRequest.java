package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PatchDeviceRequest(
        String name,
        @JsonProperty("branch_id") Long branchId,
        String ip,
        Integer puerto,
        String ubicacion,
        Boolean state
) {
}
