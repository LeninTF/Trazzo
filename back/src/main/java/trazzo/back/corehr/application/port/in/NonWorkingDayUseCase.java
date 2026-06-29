package trazzo.back.corehr.application.port.in;

import trazzo.back.corehr.application.dto.command.CreateNonWorkingDayCommand;
import trazzo.back.corehr.application.dto.command.PatchNonWorkingDayCommand;
import trazzo.back.corehr.application.dto.result.NonWorkingDayResult;
import trazzo.back.corehr.application.dto.result.PaginatedResult;

import java.time.LocalDate;
import java.util.Optional;

public interface NonWorkingDayUseCase {
    NonWorkingDayResult create(CreateNonWorkingDayCommand command);
    Optional<NonWorkingDayResult> findById(Long id);
    PaginatedResult<NonWorkingDayResult> findAll(LocalDate dateFrom, LocalDate dateTo, Boolean isRecurring, int page, int size);
    NonWorkingDayResult patch(Long id, PatchNonWorkingDayCommand command);
    void deleteById(Long id);
}
