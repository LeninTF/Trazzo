package trazzo.back.corehr.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.corehr.application.dto.command.CreateShiftCommand;
import trazzo.back.corehr.application.dto.command.PatchShiftCommand;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.ShiftResult;
import trazzo.back.corehr.application.port.in.ShiftUseCase;
import trazzo.back.corehr.application.port.out.ScheduleRepositoryPort;
import trazzo.back.corehr.application.port.out.ShiftRepositoryPort;
import trazzo.back.corehr.domain.model.schedule.Shift;

import java.util.Optional;

@RequiredArgsConstructor
public class ShiftService implements ShiftUseCase {

    private final ShiftRepositoryPort shiftRepository;
    private final ScheduleRepositoryPort scheduleRepository;

    @Override
    public ShiftResult create(CreateShiftCommand command) {
        if (shiftRepository.existsByName(command.name())) {
            throw new IllegalArgumentException("Ya existe un turno con ese nombre");
        }
        var shift = Shift.create(command.name(), command.description());
        var saved = shiftRepository.save(shift);
        return toResult(saved);
    }

    @Override
    public Optional<ShiftResult> findById(Long id) {
        return shiftRepository.findById(id).map(this::toResult);
    }

    @Override
    public PaginatedResult<ShiftResult> findAll(String search, int page, int size, String sort) {
        var shifts = shiftRepository.findAll(search, page, size, sort);
        var total = shiftRepository.count(search);
        var results = shifts.stream().map(this::toResult).toList();
        var totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PaginatedResult<>(results, page, size, total, totalPages);
    }

    @Override
    public ShiftResult patch(Long id, PatchShiftCommand command) {
        var shift = shiftRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Turno no encontrado: " + id));
        if (command.name() != null) {
            shift.rename(command.name());
        }
        if (command.description() != null) {
            shift.updateDescription(command.description());
        }
        var saved = shiftRepository.save(shift);
        return toResult(saved);
    }

    @Override
    public void deleteById(Long id) {
        var shift = shiftRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Turno no encontrado: " + id));
        if (scheduleRepository.countActiveSchedulesByShiftId(id) > 0) {
            throw new IllegalStateException("El turno tiene schedules o asignaciones activas. No se puede eliminar.");
        }
        shiftRepository.deleteById(id);
    }

    private ShiftResult toResult(Shift shift) {
        return new ShiftResult(
                shift.getId(),
                shift.getName(),
                shift.getDescription(),
                java.util.Collections.emptyList(),
                shift.getCreatedAt(),
                shift.getUpdatedAt()
        );
    }
}
