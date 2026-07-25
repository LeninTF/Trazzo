package trazzo.back.corehr.application.port.out;

import trazzo.back.corehr.application.dto.command.BulkAssignSchedulesCommand.Item;
import trazzo.back.corehr.domain.model.schedule.UserSchedule;

import java.util.List;
import java.util.Optional;

public interface UserScheduleRepositoryPort {
    UserSchedule save(UserSchedule userSchedule);
    Optional<UserSchedule> findById(Long id);
    List<UserSchedule> findAll(Long tenantUserId, Long scheduleId, Long shiftId, int page, int size);
    long count(Long tenantUserId, Long scheduleId, Long shiftId);
    void deleteById(Long id);
    boolean existsByTenantUserId(Long tenantUserId);
    boolean existsByTenantUserIdAndScheduleId(Long tenantUserId, Long scheduleId);
    List<UserSchedule> findByTenantUserId(Long tenantUserId);
    List<UserSchedule> bulkCreate(Long scheduleId, List<Item> items);
}
