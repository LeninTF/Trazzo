package trazzo.back.saasglobal.infrastructure.adapters.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateSaasUserRequest(
        @JsonProperty("document_type") @NotBlank String documentType,
        @JsonProperty("document_value") @NotBlank String documentValue,
        @NotBlank String name,
        @JsonProperty("father_surname") @NotBlank String fatherSurname,
        @JsonProperty("mother_surname") @NotBlank String motherSurname,
        @NotBlank @Email String email,
        String phone,
        String password,
        @JsonProperty("role_ids") @NotNull List<Integer> roleIds
) {}
