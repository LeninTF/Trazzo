package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.corehr.application.port.out.NonWorkingDaysRepositoryPort;
import trazzo.back.corehr.domain.model.schedule.NonWorkingDays;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper.NonWorkingDaysMapper;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.repository.NonWorkingDaysJpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NonWorkingDaysRepositoryAdapter implements NonWorkingDaysRepositoryPort {

    private final NonWorkingDaysJpaRepository nonWorkingDaysRepo;

    @Override
    @Transactional
    public NonWorkingDays save(NonWorkingDays nonWorkingDays) {
        var entity = NonWorkingDaysMapper.toEntity(nonWorkingDays);
        var saved = nonWorkingDaysRepo.save(entity);
        return NonWorkingDaysMapper.toDomain(saved);
    }

    @Override
    public Optional<NonWorkingDays> findById(Long id) {
        return nonWorkingDaysRepo.findById(id).map(NonWorkingDaysMapper::toDomain);
    }

    @Override
    public List<NonWorkingDays> findAll(LocalDate dateFrom, LocalDate dateTo, Boolean isRecurring, int page, int size) {
        var pageable = PageRequest.of(page, size);
        return nonWorkingDaysRepo.findByDateBetweenOrIsRecurring(dateFrom, dateTo, isRecurring, pageable)
                .stream()
                .map(NonWorkingDaysMapper::toDomain)
                .toList();
    }

    @Override
    public long count(LocalDate dateFrom, LocalDate dateTo, Boolean isRecurring) {
        if (dateFrom == null && dateTo == null && isRecurring == null) {
            return nonWorkingDaysRepo.count();
        }
        return nonWorkingDaysRepo.countByDateBetweenOrIsRecurring(dateFrom, dateTo, isRecurring);
    }

    @Override
    public boolean existsByDate(LocalDate date) {
        return nonWorkingDaysRepo.existsByDate(date);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        nonWorkingDaysRepo.deleteById(id);
    }
}
