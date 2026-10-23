package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record AssignRoleRequest(
    @NotNull @JsonProperty("role_id") String roleId
) {}
