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
    Optional<IncidentResult> findById(String id);
    PaginatedResult<IncidentResult> findAll(String scope, String sedeId, String areaId,
                                             String departamentoId, String state, String tipoId,
                                             LocalDate desde, LocalDate hasta, String search,
                                             int page, int size, String sort);
    IncidentResult patch(String id, PatchIncidentCommand command);
    IncidentResult changeState(String id, IncidentStateChangeCommand command);
}
