package trazzo.back.incidents.application.port.in;

import trazzo.back.incidents.application.dto.command.CreateEvidenceCommand;
import trazzo.back.incidents.application.dto.result.IncidentEvidenceResult;

import java.util.List;

public interface EvidenceUseCase {
    IncidentEvidenceResult create(Integer incidentId, CreateEvidenceCommand command);
    List<IncidentEvidenceResult> findAllByIncidentId(Integer incidentId);
    IncidentEvidenceResult findEvidence(Integer incidentId, Integer evidenceId);
    void delete(Integer incidentId, Integer evidenceId);
}
