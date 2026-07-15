package trazzo.back.incidents.application.usecase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import trazzo.back.incidents.application.dto.command.CreateEvidenceCommand;
import trazzo.back.incidents.application.port.out.EventPublisherPort;
import trazzo.back.incidents.application.port.out.IncidentRepositoryPort;
import trazzo.back.incidents.domain.model.Incident;
import trazzo.back.shared.application.port.out.FileStoragePort;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EvidenceServiceTest {

    @Mock
    private IncidentRepositoryPort incidentRepository;

    @Mock
    private EventPublisherPort eventPublisher;

    @Mock
    private FileStoragePort fileStoragePort;

    @InjectMocks
    private EvidenceService service;

    private Incident sampleIncident() {
        return Incident.restore("inc-1", "user-1", "type-1",
                trazzo.back.incidents.domain.model.IncidentState.PENDIENTE,
                "test comment", null, null, null,
                java.util.Collections.emptyList(),
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
    }

    @Test
    void create_shouldReturnResult() {
        var cmd = new CreateEvidenceCommand("file.pdf", "key-1", "application/pdf", 1024);
        when(incidentRepository.findById("inc-1")).thenReturn(Optional.of(sampleIncident()));
        when(fileStoragePort.buildPublicUrl("key-1")).thenReturn("http://url/key-1");

        var result = service.create("inc-1", cmd);

        assertThat(result.fileName()).isEqualTo("file.pdf");
        verify(incidentRepository).save(any(Incident.class));
    }

    @Test
    void create_shouldThrowWhenIncidentNotFound() {
        var cmd = new CreateEvidenceCommand("file.pdf", "key-1", "application/pdf", 1024);
        when(incidentRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create("bad-id", cmd))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void findAllByIncidentId_shouldReturnList() {
        when(incidentRepository.findById("inc-1")).thenReturn(Optional.of(sampleIncident()));

        var result = service.findAllByIncidentId("inc-1");

        assertThat(result).isEmpty();
    }

    @Test
    void findAllByIncidentId_shouldThrowWhenNotFound() {
        when(incidentRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findAllByIncidentId("bad-id"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void delete_shouldDeleteEvidence() {
        var incident = sampleIncident();
        incident.addEvidence(trazzo.back.incidents.domain.model.IncidentEvidence.create(
                "inc-1", "file.pdf", "key-1", "application/pdf", 1024));
        when(incidentRepository.findById("inc-1")).thenReturn(Optional.of(incident));

        var evidenceId = incident.getEvidences().get(0).getId();
        service.delete("inc-1", evidenceId);

        verify(incidentRepository).save(any(Incident.class));
    }

    @Test
    void delete_shouldThrowWhenIncidentNotFound() {
        when(incidentRepository.findById("bad-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete("bad-id", "ev-id"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
