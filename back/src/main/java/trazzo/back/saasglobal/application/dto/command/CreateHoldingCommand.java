package trazzo.back.saasglobal.application.dto.command;

public record CreateHoldingCommand(
        String taxId,
        String legalName,
        String type
) {}
