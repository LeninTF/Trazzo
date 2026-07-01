package trazzo.back.saasglobal.application.dto.command;

public record UpdateFeatureCommand(
        Integer id,
        String name,
        String description
) {}
