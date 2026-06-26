package trazzo.back.saasglobal.domain.model.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PaymentTransactionTest {
    @Test
    void canInstantiate() {
        assertThat(new PaymentTransaction()).isNotNull();
    }
}
