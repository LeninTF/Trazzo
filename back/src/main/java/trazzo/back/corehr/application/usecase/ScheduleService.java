package trazzo.back.corehr.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.corehr.application.dto.command.CreateScheduleCommand;
import trazzo.back.corehr.application.dto.command.PatchScheduleCommand;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.ScheduleResult;
import trazzo.back.corehr.application.port.in.ScheduleUseCase;
import trazzo.back.corehr.application.port.out.ScheduleRepositoryPort;
import trazzo.back.corehr.application.port.out.ShiftRepositoryPort;
import trazzo.back.corehr.application.port.out.ToleranciaRepositoryPort;
import trazzo.back.corehr.application.port.out.UserScheduleRepositoryPort;
import trazzo.back.corehr.domain.model.schedule.Schedule;

import java.util.Optional;

@RequiredArgsConstructor
public class ScheduleService implements ScheduleUseCase {

    private final ScheduleRepositoryPort scheduleRepository;
    private final ShiftRepositoryPort shiftRepository;
    private final ToleranciaRepositoryPort toleranciaRepository;
    private final UserScheduleRepositoryPort userScheduleRepository;

    @Override
    public ScheduleResult create(CreateScheduleCommand command) {
        if (!shiftRepository.findById(command.shiftId()).isPresent()) {
            throw new IllegalArgumentException("Turno no encontrado: " + command.shiftId());
        }
        var schedule = Schedule.create(
                command.shiftId(), command.name(), command.description(),
                command.entryTime(), command.departureTime()
        );
        var saved = scheduleRepository.save(schedule);
        return toResult(saved);
    }

    @Override
    public Optional<ScheduleResult> findById(Long id) {
        return scheduleRepository.findById(id).map(this::toResultWithDetails);
    }

    @Override
    public PaginatedResult<ScheduleResult> findAll(Long shiftId, int page, int size, String sort) {
        var schedules = scheduleRepository.findAll(shiftId, page, size, sort);
        var total = scheduleRepository.count(shiftId);
        var results = schedules.stream().map(this::toResult).toList();
        var totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PaginatedResult<>(results, page, size, total, totalPages);
    }

    @Override
    public ScheduleResult patch(Long id, PatchScheduleCommand command) {
        var schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule no encontrado: " + id));
        if (command.name() != null) {
            schedule.rename(command.name());
        }
        if (command.description() != null) {
            schedule.updateDescription(command.description());
        }
        if (command.entryTime() != null || command.departureTime() != null) {
            var entry = command.entryTime() != null ? command.entryTime() : schedule.getEntryTime();
            var departure = command.departureTime() != null ? command.departureTime() : schedule.getDepartureTime();
            schedule.reschedule(entry, departure);
        }
        var saved = scheduleRepository.save(schedule);
        return toResult(saved);
    }

    @Override
    public void deleteById(Long id) {
        var schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule no encontrado: " + id));
        if (userScheduleRepository.count(null, id) > 0) {
            throw new IllegalStateException("El schedule tiene user_schedules o asistencias activas.");
        }
        scheduleRepository.deleteById(id);
    }

    private ScheduleResult toResult(Schedule schedule) {
        var shiftSummary = shiftRepository.findById(schedule.getShiftId())
                .map(s -> new ScheduleResult.ShiftSummary(s.getId(), s.getName()))
                .orElse(null);
        return new ScheduleResult(
                schedule.getId(),
                schedule.getShiftId(),
                shiftSummary,
                schedule.getName(),
                schedule.getDescription(),
                schedule.getEntryTime(),
                schedule.getDepartureTime(),
                java.util.Collections.emptyList(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }

    private ScheduleResult toResultWithDetails(Schedule schedule) {
        var tolerancias = toleranciaRepository.findAllByScheduleId(schedule.getId(), 0, 100)
                .stream()
                .map(t -> new trazzo.back.corehr.application.dto.result.ToleranciaResult(
                        t.getId(), t.getScheduleId(), t.getName(), t.getType(),
                        t.getMinutes(), t.getDescription(), t.isActivo(),
                        t.getCreatedAt(), t.getUpdatedAt()))
                .toList();
        var shiftSummary = shiftRepository.findById(schedule.getShiftId())
                .map(s -> new ScheduleResult.ShiftSummary(s.getId(), s.getName()))
                .orElse(null);
        return new ScheduleResult(
                schedule.getId(),
                schedule.getShiftId(),
                shiftSummary,
                schedule.getName(),
                schedule.getDescription(),
                schedule.getEntryTime(),
                schedule.getDepartureTime(),
                tolerancias,
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }
}
