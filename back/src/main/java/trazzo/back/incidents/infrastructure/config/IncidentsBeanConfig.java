package trazzo.back.incidents.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import trazzo.back.incidents.application.port.in.EvidenceUseCase;
import trazzo.back.incidents.application.port.in.IncidentTypeUseCase;
import trazzo.back.incidents.application.port.in.IncidentUseCase;
import trazzo.back.incidents.application.port.in.NotificationUseCase;
import trazzo.back.incidents.application.port.out.EventPublisherPort;
import trazzo.back.incidents.application.port.out.IncidentRepositoryPort;
import trazzo.back.incidents.application.port.out.IncidentTypeRepositoryPort;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.incidents.application.usecase.EvidenceService;
import trazzo.back.incidents.application.usecase.IncidentService;
import trazzo.back.incidents.application.usecase.IncidentTypeService;
import trazzo.back.incidents.application.usecase.NotificationService;
import trazzo.back.shared.application.port.out.FileStoragePort;

@Configuration
public class IncidentsBeanConfig {

    @Bean
    public IncidentUseCase incidentUseCase(IncidentRepositoryPort incidentRepository,
                                           IncidentTypeRepositoryPort typeRepository,
                                           TenantUserPort tenantUserPort,
                                           EventPublisherPort eventPublisher) {
        return new IncidentService(incidentRepository, typeRepository, tenantUserPort, eventPublisher);
    }

    @Bean
    public EvidenceUseCase evidenceUseCase(IncidentRepositoryPort incidentRepository,
                                           EventPublisherPort eventPublisher,
                                           FileStoragePort fileStoragePort) {
        return new EvidenceService(incidentRepository, eventPublisher, fileStoragePort);
    }

    @Bean
    public IncidentTypeUseCase incidentTypeUseCase(IncidentTypeRepositoryPort repository) {
        return new IncidentTypeService(repository);
    }

    @Bean
    public NotificationUseCase notificationUseCase(IncidentRepositoryPort incidentRepository) {
        return new NotificationService(incidentRepository);
    }
}
