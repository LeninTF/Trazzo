package trazzo.back.corehr.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChangePasswordRequest(
    @JsonProperty("current_password") String currentPassword,
    @JsonProperty("new_password") String newPassword
) {}
