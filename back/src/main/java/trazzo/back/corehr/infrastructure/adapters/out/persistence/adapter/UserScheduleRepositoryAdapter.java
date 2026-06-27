package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.corehr.application.port.out.UserScheduleRepositoryPort;
import trazzo.back.corehr.domain.model.schedule.UserSchedule;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper.UserScheduleMapper;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.repository.UserScheduleJpaRepository;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserScheduleRepositoryAdapter implements UserScheduleRepositoryPort {

    private final UserScheduleJpaRepository userScheduleRepo;

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
    public List<UserSchedule> findAll(Long tenantUserId, Long scheduleId, int page, int size) {
        var pageable = PageRequest.of(page, size);
        return userScheduleRepo.findByTenantUserIdAndScheduleId(tenantUserId, scheduleId, pageable)
                .stream()
                .map(UserScheduleMapper::toDomain)
                .toList();
    }

    @Override
    public long count(Long tenantUserId, Long scheduleId) {
        return userScheduleRepo.countByTenantUserIdAndScheduleId(tenantUserId, scheduleId);
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
    public List<UserSchedule> findByTenantUserId(Long tenantUserId) {
        return userScheduleRepo.findByTenantUserId(tenantUserId)
                .stream()
                .map(UserScheduleMapper::toDomain)
                .toList();
    }
}
