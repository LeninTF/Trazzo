package trazzo.back.corehr.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.corehr.application.dto.command.CreateNonWorkingDayCommand;
import trazzo.back.corehr.application.dto.command.PatchNonWorkingDayCommand;
import trazzo.back.corehr.application.dto.result.NonWorkingDayResult;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.port.in.NonWorkingDayUseCase;
import trazzo.back.corehr.application.port.out.NonWorkingDaysRepositoryPort;
import trazzo.back.corehr.domain.model.schedule.NonWorkingDays;

import java.time.LocalDate;
import java.util.Optional;

@RequiredArgsConstructor
public class NonWorkingDayService implements NonWorkingDayUseCase {

    private final NonWorkingDaysRepositoryPort nonWorkingDaysRepository;

    @Override
    public NonWorkingDayResult create(CreateNonWorkingDayCommand command) {
        if (nonWorkingDaysRepository.existsByDate(command.date())) {
            throw new IllegalArgumentException("Ya existe un día no laborable en esa fecha");
        }
        var nwd = NonWorkingDays.create(command.date(), command.description(), command.isRecurring());
        var saved = nonWorkingDaysRepository.save(nwd);
        return toResult(saved);
    }

    @Override
    public Optional<NonWorkingDayResult> findById(Long id) {
        return nonWorkingDaysRepository.findById(id).map(this::toResult);
    }

    @Override
    public PaginatedResult<NonWorkingDayResult> findAll(LocalDate dateFrom, LocalDate dateTo, Boolean isRecurring, int page, int size) {
        var items = nonWorkingDaysRepository.findAll(dateFrom, dateTo, isRecurring, page, size);
        var total = nonWorkingDaysRepository.count(dateFrom, dateTo, isRecurring);
        var results = items.stream().map(this::toResult).toList();
        var totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PaginatedResult<>(results, page, size, total, totalPages);
    }

    @Override
    public NonWorkingDayResult patch(Long id, PatchNonWorkingDayCommand command) {
        var nwd = nonWorkingDaysRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Día no laborable no encontrado: " + id));
        if (command.date() != null) {
            nwd = NonWorkingDays.restore(
                    nwd.getId(), command.date(), nwd.getDescription(), nwd.isRecurring(), nwd.getCreatedAt()
            );
        }
        if (command.description() != null) {
            nwd.updateDescription(command.description());
        }
        if (command.isRecurring() != null) {
            nwd = NonWorkingDays.restore(
                    nwd.getId(), nwd.getDate(), nwd.getDescription(), command.isRecurring(), nwd.getCreatedAt()
            );
        }
        var saved = nonWorkingDaysRepository.save(nwd);
        return toResult(saved);
    }

    @Override
    public void deleteById(Long id) {
        if (!nonWorkingDaysRepository.findById(id).isPresent()) {
            throw new IllegalArgumentException("Día no laborable no encontrado: " + id);
        }
        nonWorkingDaysRepository.deleteById(id);
    }

    private NonWorkingDayResult toResult(NonWorkingDays nwd) {
        return new NonWorkingDayResult(
                nwd.getId(),
                nwd.getDate(),
                nwd.getDescription(),
                nwd.isRecurring(),
                nwd.getCreatedAt()
        );
    }
}
