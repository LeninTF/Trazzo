package trazzo.back.saasglobal.application.dto.command;

public record MercadoPagoWebhookCommand(
        String notificationId,
        String type,
        String action,
        String dataId,
        String rawPayload
) {}
