package trazzo.back.saasglobal.infrastructure.adapters.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@SuppressWarnings("java:S107")
public record ShopCheckoutRequest(
        @NotNull Integer planId,
        @NotBlank String firstName,
        @NotBlank String lastNamePaterno,
        @NotBlank String lastNameMaterno,
        @NotBlank String documentType,
        @NotBlank String documentNumber,
        @NotBlank @Email String email,
        @NotBlank String phone,
        @NotBlank String ruc,
        @NotBlank String companyName,
        @NotBlank String businessName,
        String address,
        boolean anotherAdmin,
        String adminFirstName,
        String adminLastNamePaterno,
        String adminLastNameMaterno,
        String adminDocumentType,
        String adminDocumentNumber,
        String adminEmail,
        String adminPhone
) {}
