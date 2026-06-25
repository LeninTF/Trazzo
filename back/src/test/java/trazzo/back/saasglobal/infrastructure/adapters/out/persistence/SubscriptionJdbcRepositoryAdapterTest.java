package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
import trazzo.back.saasglobal.domain.model.multitenancy.Subscription;
import trazzo.back.saasglobal.domain.model.multitenancy.SubscriptionStatus;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionJdbcRepositoryAdapterTest {

    @Mock JdbcTemplate jdbc;
    @InjectMocks SubscriptionJdbcRepositoryAdapter adapter;

    private static Subscription trialSub() {
        return Subscription.restore("sub-1", 1, "tenant-1",
                LocalDate.now(), null, SubscriptionStatus.TRIAL, BigDecimal.ZERO, LocalDateTime.now());
    }

    @Test
    void save_returnsSameSubscription() {
        var sub = trialSub();

        var result = adapter.save(sub);

        assertSame(sub, result);
    }

    @Test
    void save_withDateEndReturnsSameSubscription() {
        var sub = Subscription.restore("sub-2", 1, "tenant-1",
                LocalDate.now(), LocalDate.now().plusMonths(1),
                SubscriptionStatus.ACTIVE, new BigDecimal("29.99"), LocalDateTime.now());

        var result = adapter.save(sub);

        assertSame(sub, result);
    }

    @Test
    @SuppressWarnings("unchecked")
    void findById_returnsEmptyWhenNotFound() {
        when(jdbc.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());

        Optional<Subscription> result = adapter.findById("missing");

        assertTrue(result.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findActiveByTenantId_returnsEmptyWhenNotFound() {
        when(jdbc.query(anyString(), any(RowMapper.class), any())).thenReturn(List.of());

        Optional<Subscription> result = adapter.findActiveByTenantId("tenant-1");

        assertTrue(result.isEmpty());
    }
}
