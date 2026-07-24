package trazzo.back.incidents.application.port.in;

import trazzo.back.incidents.application.dto.command.CreateEvidenceCommand;
import trazzo.back.incidents.application.dto.result.IncidentEvidenceResult;

import java.util.List;

public interface EvidenceUseCase {
    IncidentEvidenceResult create(String incidentId, CreateEvidenceCommand command);
    List<IncidentEvidenceResult> findAllByIncidentId(String incidentId);
    IncidentEvidenceResult findEvidence(String incidentId, String evidenceId);
    void delete(String incidentId, String evidenceId);
}
