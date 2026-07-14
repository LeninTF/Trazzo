package trazzo.back.saasglobal.application.dto.command;

public record SubscribeToPlanCommand(String tenantId, String payerEmail, Integer planId) {}
