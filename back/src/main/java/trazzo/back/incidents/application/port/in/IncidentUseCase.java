package trazzo.back.incidents.application.port.in;

import trazzo.back.incidents.application.dto.command.CreateIncidentCommand;
import trazzo.back.incidents.application.dto.command.IncidentStateChangeCommand;
import trazzo.back.incidents.application.dto.command.PatchIncidentCommand;
import trazzo.back.incidents.application.dto.result.IncidentResult;
import trazzo.back.incidents.application.dto.result.PaginatedResult;

import java.time.LocalDate;
import java.util.Optional;

public interface IncidentUseCase {
    IncidentResult create(CreateIncidentCommand command);
    Optional<IncidentResult> findById(Integer id);
    PaginatedResult<IncidentResult> findAll(Integer currentTenantUserId, String scope, Integer sedeId, Integer areaId,
                                             Integer departamentoId, String state, Integer tipoId,
                                             LocalDate desde, LocalDate hasta, String search,
                                             int page, int size, String sort);
    IncidentResult patch(Integer id, PatchIncidentCommand command);
    IncidentResult changeState(Integer id, IncidentStateChangeCommand command);
}
