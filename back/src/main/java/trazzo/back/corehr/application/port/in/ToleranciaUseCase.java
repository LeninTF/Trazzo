package trazzo.back.corehr.application.port.in;

import trazzo.back.corehr.application.dto.command.CreateToleranciaCommand;
import trazzo.back.corehr.application.dto.command.PatchToleranciaCommand;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.ToleranciaResult;

public interface ToleranciaUseCase {
    ToleranciaResult create(Long scheduleId, CreateToleranciaCommand command);
    PaginatedResult<ToleranciaResult> findAllByScheduleId(Long scheduleId, int page, int size);
    ToleranciaResult patch(Long scheduleId, Long toleranciaId, PatchToleranciaCommand command);
    void deleteById(Long scheduleId, Long toleranciaId);
}
