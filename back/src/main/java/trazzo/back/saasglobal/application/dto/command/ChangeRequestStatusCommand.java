package trazzo.back.saasglobal.application.dto.command;

public record ChangeRequestStatusCommand(
        Integer requestId,
        String status,
        String adminUserId,
        String comment
) {}
