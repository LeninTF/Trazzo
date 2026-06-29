package trazzo.back.incidents.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.incidents.application.dto.command.CreateEvidenceCommand;
import trazzo.back.incidents.application.dto.result.IncidentEvidenceResult;
import trazzo.back.incidents.application.port.in.EvidenceUseCase;
import trazzo.back.incidents.application.port.out.EventPublisherPort;
import trazzo.back.incidents.application.port.out.IncidentRepositoryPort;
import trazzo.back.incidents.domain.model.IncidentEvidence;

import java.util.List;

@RequiredArgsConstructor
public class EvidenceService implements EvidenceUseCase {

    private final IncidentRepositoryPort incidentRepository;
    private final EventPublisherPort eventPublisher;

    @Override
    public IncidentEvidenceResult create(String incidentId, CreateEvidenceCommand command) {
        var incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incidencia no encontrada: " + incidentId));

        var evidence = IncidentEvidence.create(incidentId, command.fileName(), command.fileUrl(),
                command.mimeType(), command.fileSize());
        incident.addEvidence(evidence);

        incidentRepository.save(incident);

        var events = incident.pullDomainEvents();
        events.forEach(eventPublisher::publish);

        return toResult(evidence);
    }

    @Override
    public List<IncidentEvidenceResult> findAllByIncidentId(String incidentId) {
        var incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incidencia no encontrada: " + incidentId));

        return incident.getEvidences().stream()
                .filter(e -> !e.isDeleted())
                .map(this::toResult)
                .toList();
    }

    @Override
    public void delete(String incidentId, String evidenceId) {
        var incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incidencia no encontrada: " + incidentId));

        incident.deleteEvidence(evidenceId);
        incidentRepository.save(incident);

        var events = incident.pullDomainEvents();
        events.forEach(eventPublisher::publish);
    }

    private IncidentEvidenceResult toResult(IncidentEvidence evidence) {
        return new IncidentEvidenceResult(
                evidence.getId(),
                evidence.getIncidentId(),
                evidence.getFileName(),
                evidence.getFileUrl(),
                evidence.getMimeType(),
                evidence.getFileSize(),
                evidence.getCreatedAt(),
                evidence.getUpdatedAt()
        );
    }
}
