package trazzo.back.saasglobal.application.dto.command;

public record UpdateHoldingCommand(
        Integer id,
        String legalName,
        String type
) {}
