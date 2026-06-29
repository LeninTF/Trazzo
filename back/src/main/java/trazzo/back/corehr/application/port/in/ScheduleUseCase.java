package trazzo.back.corehr.application.port.in;

import trazzo.back.corehr.application.dto.command.CreateScheduleCommand;
import trazzo.back.corehr.application.dto.command.PatchScheduleCommand;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.ScheduleResult;

import java.util.Optional;

public interface ScheduleUseCase {
    ScheduleResult create(CreateScheduleCommand command);
    Optional<ScheduleResult> findById(Long id);
    PaginatedResult<ScheduleResult> findAll(Long shiftId, int page, int size, String sort);
    ScheduleResult patch(Long id, PatchScheduleCommand command);
    void deleteById(Long id);
}
