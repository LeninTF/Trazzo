package trazzo.back.saasglobal.application.port.out;

import java.util.Optional;
import trazzo.back.saasglobal.domain.model.invoice.PaymentTransaction;

public interface PaymentTransactionRepositoryPort {
    PaymentTransaction save(PaymentTransaction transaction);
    Optional<PaymentTransaction> findByMpPaymentId(String mpPaymentId);
}
