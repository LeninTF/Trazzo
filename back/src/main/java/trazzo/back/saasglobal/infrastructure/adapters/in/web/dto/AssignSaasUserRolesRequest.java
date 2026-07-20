package trazzo.back.saasglobal.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AssignSaasUserRolesRequest(@JsonProperty("role_ids") @NotNull List<Integer> roleIds) {}
