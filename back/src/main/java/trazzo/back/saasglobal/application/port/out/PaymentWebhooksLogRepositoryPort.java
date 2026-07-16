package trazzo.back.saasglobal.application.port.out;

import trazzo.back.saasglobal.domain.model.invoice.PaymentWebhooksLog;

public interface PaymentWebhooksLogRepositoryPort {
    /**
     * Inserts a new log row if this notification id hasn't been seen before.
     *
     * @return true if the caller should (re)attempt processing — either the id was new, or it
     * was already logged but a prior attempt never reached {@link #markProcessed}; false if a
     * prior attempt already completed successfully, so this delivery (a Mercado Pago retry)
     * must not be reprocessed.
     */
    boolean insertOrShouldRetry(PaymentWebhooksLog log);

    void markProcessed(String id);
}
