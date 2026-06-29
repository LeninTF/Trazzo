package trazzo.back.incidents.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.incidents.application.dto.command.CreateEvidenceCommand;
import trazzo.back.incidents.application.port.out.EventPublisherPort;
import trazzo.back.incidents.application.port.out.IncidentRepositoryPort;
import trazzo.back.incidents.domain.model.Incident;
import trazzo.back.incidents.domain.model.IncidentState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class EvidenceServiceTest {

    private IncidentRepositoryPort incidentRepo;
    private EventPublisherPort eventPublisher;
    private EvidenceService service;

    @BeforeEach
    void setUp() {
        incidentRepo = mock(IncidentRepositoryPort.class);
        eventPublisher = mock(EventPublisherPort.class);
        service = new EvidenceService(incidentRepo, eventPublisher);
    }

    @Test
    void createEvidenceSuccessfully() {
        var now = LocalDateTime.now();
        var incident = Incident.restore("inc-1", "u-1", "t-1", IncidentState.PENDIENTE,
                "comment", null, null, null, List.of(), now, now);
        when(incidentRepo.findById("inc-1")).thenReturn(Optional.of(incident));
        when(incidentRepo.save(any())).thenAnswer(invocation -> invocation.<Incident>getArgument(0));

        var command = new CreateEvidenceCommand("doc.pdf", "http://url", "pdf", 100);
        var result = service.create("inc-1", command);

        assertEquals("doc.pdf", result.fileName());
        assertEquals("http://url", result.fileUrl());
        assertEquals("pdf", result.mimeType());
        assertEquals(100, result.fileSize());
        verify(eventPublisher).publish(any());
    }

    @Test
    void createEvidenceWithNonExistentIncidentThrowsException() {
        when(incidentRepo.findById("bad-id")).thenReturn(Optional.empty());

        var command = new CreateEvidenceCommand("doc.pdf", "http://url", "pdf", 100);

        assertThrows(IllegalArgumentException.class, () -> service.create("bad-id", command));
    }

    @Test
    void findAllByIncidentId() {
        var now = LocalDateTime.now();
        var incident = Incident.restore("inc-1", "u-1", "t-1", IncidentState.PENDIENTE,
                null, null, null, null, List.of(), now, now);
        incident.addEvidence(trazzo.back.incidents.domain.model.IncidentEvidence.create("inc-1", "doc.pdf", "http://url", "pdf", 100));
        when(incidentRepo.findById("inc-1")).thenReturn(Optional.of(incident));

        var results = service.findAllByIncidentId("inc-1");

        assertEquals(1, results.size());
        assertEquals("doc.pdf", results.getFirst().fileName());
    }

    @Test
    void findAllByIncidentIdExcludesDeletedEvidences() {
        var now = LocalDateTime.now();
        var evidence = trazzo.back.incidents.domain.model.IncidentEvidence.restore(
                "ev-1", "inc-1", "doc.pdf", "http://url", "pdf", 100, false, null, now, now, now);
        var incident = Incident.restore("inc-1", "u-1", "t-1", IncidentState.PENDIENTE,
                null, null, null, null, List.of(evidence), now, now);
        incident.deleteEvidence("ev-1");
        when(incidentRepo.findById("inc-1")).thenReturn(Optional.of(incident));

        var results = service.findAllByIncidentId("inc-1");

        assertTrue(results.isEmpty());
    }

    @Test
    void findAllByIncidentIdWithNonExistentIncidentThrowsException() {
        when(incidentRepo.findById("bad-id")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.findAllByIncidentId("bad-id"));
    }

    @Test
    void deleteEvidenceSuccessfully() {
        var now = LocalDateTime.now();
        var evidence = trazzo.back.incidents.domain.model.IncidentEvidence.restore(
                "ev-1", "inc-1", "doc.pdf", "http://url", "pdf", 100, false, null, now, now, now);
        var incident = Incident.restore("inc-1", "u-1", "t-1", IncidentState.PENDIENTE,
                null, null, null, null, List.of(evidence), now, now);
        when(incidentRepo.findById("inc-1")).thenReturn(Optional.of(incident));
        when(incidentRepo.save(any())).thenAnswer(invocation -> invocation.<Incident>getArgument(0));

        service.delete("inc-1", "ev-1");

        assertTrue(evidence.isDeleted());
        verify(eventPublisher).publish(any());
    }

    @Test
    void deleteEvidenceWithNonExistentIncidentThrowsException() {
        when(incidentRepo.findById("bad-id")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.delete("bad-id", "ev-1"));
    }
}
