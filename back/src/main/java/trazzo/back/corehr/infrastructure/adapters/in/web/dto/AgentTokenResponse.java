package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import trazzo.back.corehr.application.dto.result.IssuedAgentToken;

import java.time.LocalDateTime;
import java.util.UUID;

/** tenant_id, device_code y token son lo que pide Configure-Agent.ps1 en el lector. */
public record AgentTokenResponse(
        String token,
        @JsonProperty("tenant_id") UUID tenantId,
        @JsonProperty("device_code") String deviceCode,
        @JsonProperty("created_at") LocalDateTime createdAt
) {
    public static AgentTokenResponse from(IssuedAgentToken issued) {
        return new AgentTokenResponse(issued.token(), issued.tenantId(), issued.deviceCode(), issued.createdAt());
    }
}
