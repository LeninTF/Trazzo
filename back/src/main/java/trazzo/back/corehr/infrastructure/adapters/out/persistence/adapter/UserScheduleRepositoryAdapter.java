package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.corehr.application.dto.command.BulkAssignSchedulesCommand.Item;
import trazzo.back.corehr.application.port.out.ScheduleRepositoryPort;
import trazzo.back.corehr.application.port.out.UserScheduleRepositoryPort;
import trazzo.back.corehr.domain.model.schedule.Schedule;
import trazzo.back.corehr.domain.model.schedule.UserSchedule;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper.UserScheduleMapper;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.repository.UserScheduleJpaRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserScheduleRepositoryAdapter implements UserScheduleRepositoryPort {

    private final UserScheduleJpaRepository userScheduleRepo;
    private final ScheduleRepositoryPort scheduleRepository;

    @Override
    @Transactional
    public UserSchedule save(UserSchedule userSchedule) {
        var entity = UserScheduleMapper.toEntity(userSchedule);
        var saved = userScheduleRepo.save(entity);
        return UserScheduleMapper.toDomain(saved);
    }

    @Override
    public Optional<UserSchedule> findById(Long id) {
        return userScheduleRepo.findById(id).map(UserScheduleMapper::toDomain);
    }

    @Override
    public List<UserSchedule> findAll(Long tenantUserId, Long scheduleId, Long shiftId, int page, int size) {
        var pageable = PageRequest.of(page, size);
        return userScheduleRepo.findAllFiltered(tenantUserId, scheduleId, shiftId, pageable)
                .stream()
                .map(UserScheduleMapper::toDomain)
                .toList();
    }

    @Override
    public long count(Long tenantUserId, Long scheduleId, Long shiftId) {
        return userScheduleRepo.countFiltered(tenantUserId, scheduleId, shiftId);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        userScheduleRepo.deleteById(id);
    }

    @Override
    public boolean existsByTenantUserId(Long tenantUserId) {
        return userScheduleRepo.existsByTenantUserId(tenantUserId);
    }

    @Override
    public boolean existsByTenantUserIdAndScheduleId(Long tenantUserId, Long scheduleId) {
        return userScheduleRepo.existsByTenantUserIdAndScheduleId(tenantUserId, scheduleId);
    }

    @Override
    public List<UserSchedule> findByTenantUserId(Long tenantUserId) {
        return userScheduleRepo.findByTenantUserId(tenantUserId)
                .stream()
                .map(UserScheduleMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public List<UserSchedule> bulkCreate(Long scheduleId, List<Item> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        var schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule no encontrado: " + scheduleId));
        var created = new ArrayList<UserSchedule>(items.size());
        for (var item : items) {
            var entryTime = item.entryTime() != null ? item.entryTime() : schedule.getEntryTime();
            var departureTime = item.departureTime() != null ? item.departureTime() : schedule.getDepartureTime();
            userScheduleRepo.insertIfAbsent(item.tenantUserId(), scheduleId, item.description(),
                    entryTime, departureTime);
            // After insertIfAbsent (ON CONFLICT DO NOTHING semantics via the unique
            // index ux_user_schedule_tenant_schedule added in V5), fetch the row
            // back so we can record the user_schedule id in the audit.
            var existing = userScheduleRepo.existsByTenantUserIdAndScheduleId(item.tenantUserId(), scheduleId);
            if (existing) {
                var userSchedule = UserSchedule.create(item.tenantUserId(), scheduleId, item.description(),
                        entryTime, departureTime);
                // Pin domain fields with persisted values by reading back through the unique index.
                // The repo returns the persisted id via a small lookup query.
                var persisted = userScheduleRepo.findByTenantUserId(item.tenantUserId()).stream()
                        .filter(e -> e.getScheduleId().equals(scheduleId))
                        .findFirst()
                        .map(UserScheduleMapper::toDomain);
                if (persisted.isPresent()) {
                    created.add(persisted.get());
                } else {
                    created.add(userSchedule);
                }
            }
        }
        return created;
    }
}
