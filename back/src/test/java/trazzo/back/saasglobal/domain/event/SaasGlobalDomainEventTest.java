package trazzo.back.saasglobal.domain.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class SaasGlobalDomainEventTest {

    @Test
    void tenantActivatedEvent_holdsValues() {
        LocalDateTime now = LocalDateTime.now();
        var event = new TenantActivatedEvent("tenant-1", "acme", now);

        assertThat(event.tenantId()).isEqualTo("tenant-1");
        assertThat(event.subDomain()).isEqualTo("acme");
        assertThat(event.occurredAt()).isEqualTo(now);
    }

    @Test
    void subscriptionActivatedEvent_holdsValues() {
        LocalDateTime now = LocalDateTime.now();
        var event = new SubscriptionActivatedEvent("sub-1", "tenant-1", 2, now);

        assertThat(event.subscriptionId()).isEqualTo("sub-1");
        assertThat(event.tenantId()).isEqualTo("tenant-1");
        assertThat(event.planId()).isEqualTo(2);
        assertThat(event.occurredAt()).isEqualTo(now);
    }
}
