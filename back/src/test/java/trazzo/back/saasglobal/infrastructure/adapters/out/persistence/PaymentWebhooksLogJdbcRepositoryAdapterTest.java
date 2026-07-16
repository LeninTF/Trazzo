package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import trazzo.back.saasglobal.domain.model.invoice.PaymentWebhooksLog;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentWebhooksLogJdbcRepositoryAdapterTest {

    @Mock JdbcTemplate jdbc;
    @InjectMocks PaymentWebhooksLogJdbcRepositoryAdapter adapter;

    @Test
    void insertOrShouldRetry_returnsTrueWhenRowInserted() {
        when(jdbc.update(anyString(), any(), any(), any(), any(), anyBoolean(), any())).thenReturn(1);

        boolean result = adapter.insertOrShouldRetry(
                PaymentWebhooksLog.create("evt-1", "evt-1", "payment.created", "{}"));

        assertTrue(result);
        verify(jdbc, never()).query(anyString(), any(ResultSetExtractor.class), any());
    }

    @Test
    void insertOrShouldRetry_returnsTrueWhenExistingRowNotYetProcessed() {
        when(jdbc.update(anyString(), any(), any(), any(), any(), anyBoolean(), any())).thenReturn(0);
        when(jdbc.query(anyString(), any(ResultSetExtractor.class), eq("evt-1"))).thenReturn(false);

        boolean result = adapter.insertOrShouldRetry(
                PaymentWebhooksLog.create("evt-1", "evt-1", "payment.created", "{}"));

        assertTrue(result);
    }

    @Test
    void insertOrShouldRetry_returnsFalseWhenAlreadyProcessed() {
        when(jdbc.update(anyString(), any(), any(), any(), any(), anyBoolean(), any())).thenReturn(0);
        when(jdbc.query(anyString(), any(ResultSetExtractor.class), eq("evt-1"))).thenReturn(true);

        boolean result = adapter.insertOrShouldRetry(
                PaymentWebhooksLog.create("evt-1", "evt-1", "payment.created", "{}"));

        assertFalse(result);
    }

    @Test
    void markProcessed_updatesRowById() {
        adapter.markProcessed("evt-1");

        verify(jdbc).update(anyString(), eq("evt-1"));
    }
}
