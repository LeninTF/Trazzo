package trazzo.back.saasglobal.application.dto.command;

public record CreateFeatureCommand(
        String name,
        String description
) {}
