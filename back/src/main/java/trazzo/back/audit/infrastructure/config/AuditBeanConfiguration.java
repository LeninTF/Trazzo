package trazzo.back.audit.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import trazzo.back.audit.application.port.in.AuditLogUseCase;
import trazzo.back.audit.application.port.in.AuditMetricsUseCase;
import trazzo.back.audit.application.port.in.LoginHistoryUseCase;
import trazzo.back.audit.application.port.in.SessionUseCase;
import trazzo.back.audit.application.port.in.SystemAuditUseCase;
import trazzo.back.audit.application.port.in.TenantSettingsUseCase;
import trazzo.back.audit.application.port.out.AuditRepositoryPort;
import trazzo.back.audit.application.port.out.LogInHistoryRepositoryPort;
import trazzo.back.audit.application.port.out.SessionRepositoryPort;
import trazzo.back.audit.application.port.out.SystemAuditRepositoryPort;
import trazzo.back.audit.application.port.out.TenantInfoPort;
import trazzo.back.audit.application.port.out.TenantSettingsRecordRepositoryPort;
import trazzo.back.audit.application.port.out.UserInfoPort;
import trazzo.back.audit.application.usecase.AuditLogService;
import trazzo.back.audit.application.usecase.AuditMetricsService;
import trazzo.back.audit.application.usecase.LoginHistoryService;
import trazzo.back.audit.application.usecase.SessionService;
import trazzo.back.audit.application.usecase.SystemAuditService;
import trazzo.back.audit.application.usecase.TenantSettingsService;

@Configuration
public class AuditBeanConfiguration {

    @Bean
    public AuditLogUseCase auditLogUseCase(
            AuditRepositoryPort auditRepositoryPort,
            UserInfoPort userInfoPort,
            TenantInfoPort tenantInfoPort) {
        return new AuditLogService(auditRepositoryPort, userInfoPort, tenantInfoPort);
    }

    @Bean
    public SystemAuditUseCase systemAuditUseCase(SystemAuditRepositoryPort systemAuditRepositoryPort) {
        return new SystemAuditService(systemAuditRepositoryPort);
    }

    @Bean
    public LoginHistoryUseCase loginHistoryUseCase(LogInHistoryRepositoryPort logInHistoryRepositoryPort) {
        return new LoginHistoryService(logInHistoryRepositoryPort);
    }

    @Bean
    public SessionUseCase sessionUseCase(SessionRepositoryPort sessionRepositoryPort) {
        return new SessionService(sessionRepositoryPort);
    }

    @Bean
    public TenantSettingsUseCase tenantSettingsUseCase(
            TenantSettingsRecordRepositoryPort tenantSettingsRecordRepositoryPort) {
        return new TenantSettingsService(tenantSettingsRecordRepositoryPort);
    }

    @Bean
    public AuditMetricsUseCase auditMetricsUseCase(JdbcTemplate jdbcTemplate) {
        return new AuditMetricsService(jdbcTemplate);
    }
}
