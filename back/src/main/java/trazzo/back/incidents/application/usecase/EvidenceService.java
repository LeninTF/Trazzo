package trazzo.back.incidents.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.incidents.application.dto.command.CreateEvidenceCommand;
import trazzo.back.incidents.application.dto.result.IncidentEvidenceResult;
import trazzo.back.incidents.application.port.in.EvidenceUseCase;
import trazzo.back.incidents.application.port.out.EventPublisherPort;
import trazzo.back.incidents.application.port.out.IncidentRepositoryPort;
import trazzo.back.incidents.domain.model.IncidentEvidence;
import trazzo.back.shared.application.port.out.FileStoragePort;

import java.util.List;

@RequiredArgsConstructor
public class EvidenceService implements EvidenceUseCase {

    private final IncidentRepositoryPort incidentRepository;
    private final EventPublisherPort eventPublisher;
    private final FileStoragePort fileStoragePort;

    @Override
    public IncidentEvidenceResult create(String incidentId, CreateEvidenceCommand command) {
        var incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incidencia no encontrada: " + incidentId));

        var evidence = IncidentEvidence.create(incidentId, command.fileName(), command.fileKey(),
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
    public IncidentEvidenceResult findEvidence(String incidentId, String evidenceId) {
        var incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incidencia no encontrada: " + incidentId));
        var evidence = incident.getEvidences().stream()
                .filter(e -> e.getId() != null && e.getId().equals(evidenceId) && !e.isDeleted())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Evidencia no encontrada: " + evidenceId + " en incidencia: " + incidentId));
        return toResult(evidence);
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
        String downloadUrl = "/api/v1/incidentes/" + evidence.getIncidentId()
                + "/evidencias/" + evidence.getId() + "/descarga";
        return new IncidentEvidenceResult(
                evidence.getId(),
                evidence.getIncidentId(),
                evidence.getFileName(),
                evidence.getFileKey(),
                downloadUrl,
                evidence.getMimeType(),
                evidence.getFileSize(),
                evidence.getCreatedAt(),
                evidence.getUpdatedAt()
        );
    }
}
