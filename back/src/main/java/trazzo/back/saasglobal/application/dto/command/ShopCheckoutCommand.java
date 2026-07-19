package trazzo.back.saasglobal.application.dto.command;

@SuppressWarnings("java:S107")
public record ShopCheckoutCommand(
        Integer planId,
        String firstName,
        String lastNamePaterno,
        String lastNameMaterno,
        String documentType,
        String documentNumber,
        String email,
        String phone,
        String ruc,
        String companyName,
        String businessName,
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
