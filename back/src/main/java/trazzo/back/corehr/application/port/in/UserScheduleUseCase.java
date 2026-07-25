package trazzo.back.corehr.application.port.in;

import trazzo.back.corehr.application.dto.command.BulkAssignSchedulesCommand;
import trazzo.back.corehr.application.dto.result.BulkAssignSchedulesResult;
import trazzo.back.corehr.application.dto.command.CreateUserScheduleCommand;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.UserScheduleResult;

import java.util.List;
import java.util.Optional;

public interface UserScheduleUseCase {
    UserScheduleResult create(CreateUserScheduleCommand command);
    BulkAssignSchedulesResult bulkAssign(BulkAssignSchedulesCommand command);
    PaginatedResult<UserScheduleResult> findAll(Long tenantUserId, Long scheduleId, Long shiftId, int page, int size);
    List<UserScheduleResult> findByTenantUserId(Long tenantUserId);
    Optional<UserScheduleResult> findById(Long id);
    void deleteById(Long id);
}
