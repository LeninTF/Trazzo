package trazzo.back.corehr.application.port.out;

import trazzo.back.corehr.domain.model.schedule.Schedule;

import java.util.List;
import java.util.Optional;

public interface ScheduleRepositoryPort {
    Schedule save(Schedule schedule);
    Optional<Schedule> findById(Long id);
    List<Schedule> findAll(Long shiftId, int page, int size, String sort);
    long count(Long shiftId);
    boolean existsByShiftId(Long shiftId);
    void deleteById(Long id);
    long countActiveSchedulesByShiftId(Long shiftId);
}
