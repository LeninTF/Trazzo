package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import trazzo.back.saasglobal.application.port.out.PaymentWebhooksLogRepositoryPort;
import trazzo.back.saasglobal.domain.model.invoice.PaymentWebhooksLog;

@Repository
@RequiredArgsConstructor
public class PaymentWebhooksLogJdbcRepositoryAdapter implements PaymentWebhooksLogRepositoryPort {

    private final JdbcTemplate jdbc;

    @Override
    public boolean insertIfNotExists(PaymentWebhooksLog log) {
        int rowsInserted = jdbc.update("""
                INSERT INTO payment_webhooks_log (id, mp_event_id, action, raw_payload, processed, received_at)
                VALUES (?, ?, ?, CAST(? AS jsonb), ?, ?)
                ON CONFLICT (id) DO NOTHING
                """,
                log.getId(),
                log.getMpEventId(),
                log.getAction(),
                log.getRawPayload(),
                log.isProcessed(),
                log.getReceivedAt());
        return rowsInserted > 0;
    }

    @Override
    public void markProcessed(String id) {
        jdbc.update("UPDATE payment_webhooks_log SET processed = TRUE WHERE id = ?", id);
    }
}
