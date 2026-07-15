package trazzo.back.audit.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.audit.application.port.out.AuditRepositoryPort;
import trazzo.back.audit.application.port.out.LogInHistoryRepositoryPort;
import trazzo.back.audit.application.port.out.SessionRepositoryPort;
import trazzo.back.audit.application.port.out.SystemAuditRepositoryPort;
import trazzo.back.audit.application.port.out.TenantInfoPort;
import trazzo.back.audit.application.port.out.TenantSettingsRecordRepositoryPort;
import trazzo.back.audit.application.port.out.UserInfoPort;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class AuditBeanConfigurationTest {

    private final AuditBeanConfiguration config = new AuditBeanConfiguration();

    @Mock private AuditRepositoryPort auditRepositoryPort;
    @Mock private UserInfoPort userInfoPort;
    @Mock private TenantInfoPort tenantInfoPort;
    @Mock private SystemAuditRepositoryPort systemAuditRepositoryPort;
    @Mock private LogInHistoryRepositoryPort logInHistoryRepositoryPort;
    @Mock private SessionRepositoryPort sessionRepositoryPort;
    @Mock private TenantSettingsRecordRepositoryPort tenantSettingsRecordRepositoryPort;

    @Test
    void shouldCreateAuditLogUseCase() {
        assertNotNull(config.auditLogUseCase(auditRepositoryPort, userInfoPort, tenantInfoPort));
    }

    @Test
    void shouldCreateSystemAuditUseCase() {
        assertNotNull(config.systemAuditUseCase(systemAuditRepositoryPort));
    }

    @Test
    void shouldCreateLoginHistoryUseCase() {
        assertNotNull(config.loginHistoryUseCase(logInHistoryRepositoryPort));
    }

    @Test
    void shouldCreateSessionUseCase() {
        assertNotNull(config.sessionUseCase(sessionRepositoryPort));
    }

    @Test
    void shouldCreateTenantSettingsUseCase() {
        assertNotNull(config.tenantSettingsUseCase(tenantSettingsRecordRepositoryPort));
    }

    @Test
    void shouldCreateAuditMetricsUseCase() {
        assertNotNull(config.auditMetricsUseCase(auditRepositoryPort));
    }
}
