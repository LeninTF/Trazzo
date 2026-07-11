package trazzo.back.organization.application.dto.command;

public record CreatePermissionCommand(String name, String description, String masterFeaturesCode) {}
