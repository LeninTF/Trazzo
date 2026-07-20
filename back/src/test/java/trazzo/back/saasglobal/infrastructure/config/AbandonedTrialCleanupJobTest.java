package trazzo.back.saasglobal.infrastructure.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import trazzo.back.saasglobal.application.port.out.TenantRepositoryPort;
import trazzo.back.saasglobal.application.usecase.TenantPurgeService;
import trazzo.back.saasglobal.domain.model.multitenancy.Tenant;
import trazzo.back.saasglobal.domain.model.multitenancy.TenantSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AbandonedTrialCleanupJobTest {

    @Mock TenantRepositoryPort tenantRepository;
    @Mock TenantPurgeService tenantPurgeService;
    @InjectMocks AbandonedTrialCleanupJob job;

    private static Tenant tenant(String id) {
        return Tenant.restore(id, null, "acme-" + id, 2,
                TenantSettings.of(id, "tenant_acme_" + id), null, LocalDateTime.now(), null,
                LocalDateTime.now(), LocalDateTime.now(), null);
    }

    @Test
    void purgeAbandonedTrials_purgesEachAbandonedTenant() {
        ReflectionTestUtils.setField(job, "abandonedTrialHours", 24);
        when(tenantRepository.findAbandonedTrials(any())).thenReturn(List.of(tenant("t-1"), tenant("t-2")));

        job.purgeAbandonedTrials();

        verify(tenantPurgeService).purge("t-1");
        verify(tenantPurgeService).purge("t-2");
    }

    @Test
    void purgeAbandonedTrials_doesNothingWhenNoneAbandoned() {
        ReflectionTestUtils.setField(job, "abandonedTrialHours", 24);
        when(tenantRepository.findAbandonedTrials(any())).thenReturn(List.of());

        job.purgeAbandonedTrials();

        verify(tenantPurgeService, never()).purge(anyString());
    }
}
