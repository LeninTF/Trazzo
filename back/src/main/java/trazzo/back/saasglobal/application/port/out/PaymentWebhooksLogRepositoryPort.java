package trazzo.back.saasglobal.application.port.out;

import trazzo.back.saasglobal.domain.model.invoice.PaymentWebhooksLog;

public interface PaymentWebhooksLogRepositoryPort {
    /**
     * @return true if this notification id was new and got inserted; false if it was already
     * logged (a Mercado Pago retry of a notification already seen — caller must not reprocess it).
     */
    boolean insertIfNotExists(PaymentWebhooksLog log);

    void markProcessed(String id);
}
