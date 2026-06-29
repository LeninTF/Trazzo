package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.corehr.application.port.out.ScheduleRepositoryPort;
import trazzo.back.corehr.domain.model.schedule.Schedule;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper.ScheduleMapper;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.repository.ScheduleJpaRepository;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleRepositoryAdapter implements ScheduleRepositoryPort {

    private final ScheduleJpaRepository scheduleRepo;

    @Override
    @Transactional
    public Schedule save(Schedule schedule) {
        var entity = ScheduleMapper.toEntity(schedule);
        var saved = scheduleRepo.save(entity);
        return ScheduleMapper.toDomain(saved);
    }

    @Override
    public Optional<Schedule> findById(Long id) {
        return scheduleRepo.findById(id).map(ScheduleMapper::toDomain);
    }

    @Override
    public List<Schedule> findAll(Long shiftId, int page, int size, String sort) {
        var sortObj = SortUtils.parseSort(sort, f -> switch (f) {
            case "name" -> "name";
            case "entry_time", "entryTime" -> "entryTime";
            case "departure_time", "departureTime" -> "departureTime";
            case "created_at", "createdAt" -> "createdAt";
            case "updated_at", "updatedAt" -> "updatedAt";
            default -> "createdAt";
        });
        var pageable = PageRequest.of(page, size, sortObj);
        return (shiftId == null
                ? scheduleRepo.findAll(pageable)
                : scheduleRepo.findByShiftId(shiftId, pageable))
                .stream()
                .map(ScheduleMapper::toDomain)
                .toList();
    }

    @Override
    public long count(Long shiftId) {
        if (shiftId == null) {
            return scheduleRepo.count();
        }
        return scheduleRepo.countByShiftId(shiftId);
    }

    @Override
    public boolean existsByShiftId(Long shiftId) {
        return scheduleRepo.existsByShiftId(shiftId);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        scheduleRepo.deleteById(id);
    }

    @Override
    public long countActiveSchedulesByShiftId(Long shiftId) {
        return scheduleRepo.countByShiftId(shiftId);
    }

}
