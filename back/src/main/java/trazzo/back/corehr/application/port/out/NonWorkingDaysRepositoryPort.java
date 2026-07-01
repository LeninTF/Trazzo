package trazzo.back.corehr.application.port.out;

import trazzo.back.corehr.domain.model.schedule.NonWorkingDays;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NonWorkingDaysRepositoryPort {
    NonWorkingDays save(NonWorkingDays nonWorkingDays);
    Optional<NonWorkingDays> findById(Long id);
    List<NonWorkingDays> findAll(LocalDate dateFrom, LocalDate dateTo, Boolean isRecurring, int page, int size);
    long count(LocalDate dateFrom, LocalDate dateTo, Boolean isRecurring);
    boolean existsByDate(LocalDate date);
    void deleteById(Long id);
}
