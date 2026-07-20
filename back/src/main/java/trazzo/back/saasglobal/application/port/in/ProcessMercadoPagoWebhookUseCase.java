package trazzo.back.saasglobal.application.port.in;

import trazzo.back.saasglobal.application.dto.command.MercadoPagoWebhookCommand;

public interface ProcessMercadoPagoWebhookUseCase {
    void process(MercadoPagoWebhookCommand command);
}
