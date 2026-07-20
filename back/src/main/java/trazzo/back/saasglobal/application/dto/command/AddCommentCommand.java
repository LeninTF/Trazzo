package trazzo.back.saasglobal.application.dto.command;

public record AddCommentCommand(
        Integer requestId,
        String adminUserId,
        String comment
) {}
