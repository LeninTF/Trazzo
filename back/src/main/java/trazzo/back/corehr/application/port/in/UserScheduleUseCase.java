package trazzo.back.corehr.application.port.in;

import trazzo.back.corehr.application.dto.command.CreateUserScheduleCommand;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.UserScheduleResult;

public interface UserScheduleUseCase {
    UserScheduleResult create(CreateUserScheduleCommand command);
    PaginatedResult<UserScheduleResult> findAll(Long tenantUserId, Long scheduleId, int page, int size);
    void deleteById(Long id);
}
