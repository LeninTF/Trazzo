package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import trazzo.back.saasglobal.application.port.out.PaymentTransactionRepositoryPort;
import trazzo.back.saasglobal.domain.model.invoice.PaymentTransaction;

@Repository
@RequiredArgsConstructor
public class PaymentTransactionJdbcRepositoryAdapter implements PaymentTransactionRepositoryPort {

    private final JdbcTemplate jdbc;

    @Override
    public PaymentTransaction save(PaymentTransaction transaction) {
        jdbc.update("""
                INSERT INTO payment_transactions
                    (id, tenant_id, subscription_id, mp_preference_id, mp_payment_id,
                     amount, net_amount, status_payment, created_at)
                VALUES (?::uuid, ?::uuid, ?::uuid, ?, ?, ?, ?, CAST(? AS status_payment_enum), ?)
                ON CONFLICT (id) DO UPDATE SET
                    mp_payment_id  = EXCLUDED.mp_payment_id,
                    status_payment = EXCLUDED.status_payment
                """,
                transaction.getId(),
                transaction.getTenantId(),
                transaction.getSubscriptionId(),
                transaction.getMpPreferenceId(),
                transaction.getMpPaymentId(),
                transaction.getAmount(),
                transaction.getNetAmount(),
                transaction.getPaymentStatus(),
                transaction.getCreatedAt());
        return transaction;
    }

    @Override
    public Optional<PaymentTransaction> findByMpPaymentId(String mpPaymentId) {
        List<PaymentTransaction> rows = jdbc.query(
                "SELECT * FROM payment_transactions WHERE mp_payment_id = ?", this::mapRow, mpPaymentId);
        return rows.stream().findFirst();
    }

    private PaymentTransaction mapRow(ResultSet rs, int rowNum) throws SQLException {
        return PaymentTransaction.restore(
                rs.getString("id"),
                rs.getString("tenant_id"),
                rs.getString("subscription_id"),
                rs.getString("mp_preference_id"),
                rs.getString("mp_payment_id"),
                rs.getBigDecimal("amount"),
                rs.getBigDecimal("net_amount"),
                rs.getString("status_payment"),
                rs.getObject("created_at", LocalDateTime.class));
    }
}
