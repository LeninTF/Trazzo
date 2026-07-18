package trazzo.back.saasglobal.application.dto.result;

public record RequestContactResult(
        String name,
        String lastName,
        String email,
        String phoneNumber,
        String taxId,
        String companyName
) {}
