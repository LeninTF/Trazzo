package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BulkAssignUserSchedulesRequest(
        @NotEmpty @Size(max = 500) @JsonProperty("tenant_user_ids") List<Long> tenantUserIds,
        @NotNull @JsonProperty("schedule_id") Long scheduleId,
        String description
) {
}
