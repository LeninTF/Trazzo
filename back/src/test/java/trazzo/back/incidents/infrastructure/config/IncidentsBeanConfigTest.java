package trazzo.back.incidents.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.incidents.application.port.out.EventPublisherPort;
import trazzo.back.incidents.application.port.out.IncidentRepositoryPort;
import trazzo.back.incidents.application.port.out.IncidentTypeRepositoryPort;
import trazzo.back.shared.application.port.out.FileStoragePort;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class IncidentsBeanConfigTest {

    private final IncidentsBeanConfig config = new IncidentsBeanConfig();

    @Mock private IncidentRepositoryPort incidentRepo;
    @Mock private IncidentTypeRepositoryPort typeRepo;
    @Mock private TenantUserPort tenantUserPort;
    @Mock private EventPublisherPort eventPublisher;
    @Mock private FileStoragePort fileStoragePort;

    @Test
    void shouldCreateIncidentUseCase() {
        assertNotNull(config.incidentUseCase(incidentRepo, typeRepo, tenantUserPort, eventPublisher));
    }

    @Test
    void shouldCreateEvidenceUseCase() {
        assertNotNull(config.evidenceUseCase(incidentRepo, eventPublisher, fileStoragePort));
    }

    @Test
    void shouldCreateIncidentTypeUseCase() {
        assertNotNull(config.incidentTypeUseCase(typeRepo));
    }

    @Test
    void shouldCreateNotificationUseCase() {
        assertNotNull(config.notificationUseCase(incidentRepo));
    }
}
