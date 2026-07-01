package trazzo.back.corehr.application.port.out;

import trazzo.back.corehr.domain.model.schedule.UserSchedule;

import java.util.List;
import java.util.Optional;

public interface UserScheduleRepositoryPort {
    UserSchedule save(UserSchedule userSchedule);
    Optional<UserSchedule> findById(Long id);
    List<UserSchedule> findAll(Long tenantUserId, Long scheduleId, int page, int size);
    long count(Long tenantUserId, Long scheduleId);
    void deleteById(Long id);
    boolean existsByTenantUserId(Long tenantUserId);
    List<UserSchedule> findByTenantUserId(Long tenantUserId);
}
