package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.corehr.application.dto.result.DeviceResult;

import java.time.LocalDateTime;

public record DeviceResponse(
        Long id,
        String code,
        String name,
        @JsonProperty("branch_id") Long branchId,
        @JsonProperty("branch_name") String branchName,
        String ip,
        Integer puerto,
        String ubicacion,
        boolean state,
        @JsonProperty("created_at") LocalDateTime createdAt
) {
    public static DeviceResponse from(DeviceResult result) {
        return new DeviceResponse(result.id(), result.code(), result.name(), result.branchId(),
                result.branchName(), result.ip(), result.puerto(), result.ubicacion(),
                result.state(), result.createdAt());
    }
}
