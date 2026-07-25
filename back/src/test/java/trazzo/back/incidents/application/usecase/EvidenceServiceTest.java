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
import trazzo.back.incidents.domain.model.IncidentEvidence;
import trazzo.back.incidents.domain.model.IncidentState;
import trazzo.back.shared.application.port.out.FileStoragePort;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
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
        return Incident.restore(1, 1, 1,
                IncidentState.PENDIENTE,
                "test comment", null, null, null,
                Collections.emptyList(),
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void createEvidenceSuccessfully() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(1, 1, 1, IncidentState.PENDIENTE,
                "comment", null, null, null, List.of(), now, now);
        when(incidentRepository.findById(1)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any())).thenAnswer(invocation -> {
            Incident inc = invocation.getArgument(0);
            Integer incId = inc.getId() != null ? inc.getId() : 1;
            var ev = inc.getEvidences().get(0);
            var persistedEv = IncidentEvidence.restore(10, incId, ev.getFileName(), ev.getFileKey(), ev.getMimeType(), ev.getFileSize(), false, null, ev.getUploadedAt(), ev.getCreatedAt(), ev.getUpdatedAt());
            return Incident.restore(incId, inc.getTenantUserId(), inc.getIncidentTypeId(), inc.getState(), inc.getComment(), inc.getRejectionReason(), inc.getType(), inc.getPermission(), List.of(persistedEv), inc.getCreatedAt(), inc.getUpdatedAt());
        });

        var command = new CreateEvidenceCommand("doc.pdf", "file-key", "pdf", 100);
        var result = service.create(1, command);

        assertEquals("doc.pdf", result.fileName());
        assertEquals("pdf", result.mimeType());
        assertEquals(100, result.fileSize());
        verify(eventPublisher).publish(any());
    }

    @Test
    void createEvidenceWithNonExistentIncidentThrowsException() {
        when(incidentRepository.findById(999)).thenReturn(Optional.empty());

        var command = new CreateEvidenceCommand("doc.pdf", "http://url", "pdf", 100);

        assertThrows(IllegalArgumentException.class, () -> service.create(999, command));
    }

    @Test
    void findAllByIncidentId() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(1, 1, 1, IncidentState.PENDIENTE,
                null, null, null, null, List.of(), now, now);
        incident.addEvidence(IncidentEvidence.create(1, "doc.pdf", "http://url", "pdf", 100));
        when(incidentRepository.findById(1)).thenReturn(Optional.of(incident));

        var results = service.findAllByIncidentId(1);

        assertThat(results).hasSize(1);
    }

    @Test
    void findAllByIncidentIdExcludesDeletedEvidences() {
        var now = LocalDateTime.now();
        var evidence = IncidentEvidence.restore(
                1, 1, "doc.pdf", "http://url", "pdf", 100, false, null, now, now, now);
        var incident = Incident.restore(1, 1, 1, IncidentState.PENDIENTE,
                null, null, null, null, List.of(evidence), now, now);
        incident.deleteEvidence(1);
        when(incidentRepository.findById(1)).thenReturn(Optional.of(incident));

        var results = service.findAllByIncidentId(1);

        assertTrue(results.isEmpty());
    }

    @Test
    void findAllByIncidentIdWithNonExistentIncidentThrowsException() {
        when(incidentRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.findAllByIncidentId(999));
    }

    @Test
    void findEvidenceSuccessfully() {
        var now = LocalDateTime.now();
        var evidence = IncidentEvidence.restore(
                1, 1, "doc.pdf", "file-key", "application/pdf", 100, false, null, now, now, now);
        var incident = Incident.restore(1, 1, 1, IncidentState.PENDIENTE,
                null, null, null, null, List.of(evidence), now, now);
        when(incidentRepository.findById(1)).thenReturn(Optional.of(incident));

        var result = service.findEvidence(1, 1);

        assertEquals(1, result.id());
        assertEquals("doc.pdf", result.fileName());
        assertEquals("file-key", result.fileKey());
        assertEquals("/api/v1/incidentes/1/evidencias/1/descarga", result.downloadUrl());
    }

    @Test
    void findEvidenceWithNonExistentIncidentThrowsException() {
        when(incidentRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.findEvidence(999, 1));
    }

    @Test
    void findEvidenceWithNonExistentEvidenceIdThrowsException() {
        var now = LocalDateTime.now();
        var incident = Incident.restore(1, 1, 1, IncidentState.PENDIENTE,
                null, null, null, null, List.of(), now, now);
        when(incidentRepository.findById(1)).thenReturn(Optional.of(incident));

        assertThrows(IllegalArgumentException.class, () -> service.findEvidence(1, 999));
    }

    @Test
    void findEvidenceExcludesDeletedEvidences() {
        var now = LocalDateTime.now();
        var evidence = IncidentEvidence.restore(
                1, 1, "doc.pdf", "file-key", "application/pdf", 100, true, now, now, now, now);
        var incident = Incident.restore(1, 1, 1, IncidentState.PENDIENTE,
                null, null, null, null, List.of(evidence), now, now);
        when(incidentRepository.findById(1)).thenReturn(Optional.of(incident));

        assertThrows(IllegalArgumentException.class, () -> service.findEvidence(1, 1));
    }

    @Test
    void deleteEvidenceSuccessfully() {
        var now = LocalDateTime.now();
        var evidence = IncidentEvidence.restore(
                1, 1, "doc.pdf", "http://url", "pdf", 100, false, null, now, now, now);
        var incident = Incident.restore(1, 1, 1, IncidentState.PENDIENTE,
                null, null, null, null, List.of(evidence), now, now);
        when(incidentRepository.findById(1)).thenReturn(Optional.of(incident));
        when(incidentRepository.save(any())).thenAnswer(invocation -> invocation.<Incident>getArgument(0));

        service.delete(1, 1);

        assertTrue(evidence.isDeleted());
        verify(eventPublisher).publish(any());
    }

    @Test
    void deleteEvidenceWithNonExistentIncidentThrowsException() {
        when(incidentRepository.findById(999)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.delete(999, 1));
    }
}
