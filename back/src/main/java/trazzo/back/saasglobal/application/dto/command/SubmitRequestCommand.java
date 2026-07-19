package trazzo.back.saasglobal.application.dto.command;

public record SubmitRequestCommand(
        String type,
        String name,
        String lastName,
        String email,
        String phoneNumber,
        String taxId,
        String companyName,
        String message
) {}
