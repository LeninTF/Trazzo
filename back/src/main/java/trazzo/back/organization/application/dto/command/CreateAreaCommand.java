package trazzo.back.organization.application.dto.command;

public record CreateAreaCommand(Long branchId, String name, String description) {}
