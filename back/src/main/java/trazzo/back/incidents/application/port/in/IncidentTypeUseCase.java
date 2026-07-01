package trazzo.back.incidents.application.port.in;

import trazzo.back.incidents.application.dto.command.CreateIncidentTypeCommand;
import trazzo.back.incidents.application.dto.command.PatchIncidentTypeCommand;
import trazzo.back.incidents.application.dto.result.IncidentTypeResult;
import trazzo.back.incidents.application.dto.result.PaginatedResult;

import java.util.Optional;

public interface IncidentTypeUseCase {
    IncidentTypeResult create(CreateIncidentTypeCommand command);
    Optional<IncidentTypeResult> findById(String id);
    PaginatedResult<IncidentTypeResult> findAll(Boolean activo, int page, int size);
    IncidentTypeResult patch(String id, PatchIncidentTypeCommand command);
}
