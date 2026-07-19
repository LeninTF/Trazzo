package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import trazzo.back.saasglobal.domain.model.invoice.PaymentTransaction;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentTransactionJdbcRepositoryAdapterTest {

    @Mock JdbcTemplate jdbc;
    @InjectMocks PaymentTransactionJdbcRepositoryAdapter adapter;

    private static PaymentTransaction transaction() {
        return PaymentTransaction.create("tenant-1", "sub-1", "pref-1", new BigDecimal("29.99"), new BigDecimal("28.50"));
    }

    @Test
    void save_returnsSameTransaction() {
        var tx = transaction();

        var result = adapter.save(tx);

        assertSame(tx, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findByMpPaymentId_returnsEmptyWhenNotFound() {
        when(jdbc.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());

        Optional<PaymentTransaction> result = adapter.findByMpPaymentId("missing");

        assertTrue(result.isEmpty());
    }
}
