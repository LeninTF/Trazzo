package trazzo.back.corehr.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.corehr.application.dto.command.CreateToleranciaCommand;
import trazzo.back.corehr.application.dto.command.PatchToleranciaCommand;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.dto.result.ToleranciaResult;
import trazzo.back.corehr.application.port.in.ToleranciaUseCase;
import trazzo.back.corehr.application.port.out.ScheduleRepositoryPort;
import trazzo.back.corehr.application.port.out.ToleranciaRepositoryPort;
import trazzo.back.corehr.domain.model.schedule.Tolerancia;

@RequiredArgsConstructor
public class ToleranciaService implements ToleranciaUseCase {

    private final ToleranciaRepositoryPort toleranciaRepository;
    private final ScheduleRepositoryPort scheduleRepository;

    @Override
    public ToleranciaResult create(Long scheduleId, CreateToleranciaCommand command) {
        if (!scheduleRepository.findById(scheduleId).isPresent()) {
            throw new IllegalArgumentException("Schedule no encontrado: " + scheduleId);
        }
        if (toleranciaRepository.existsActiveByScheduleIdAndType(scheduleId, command.type())) {
            throw new IllegalStateException("Ya existe una tolerancia activa del mismo tipo para este schedule");
        }
        var tolerancia = Tolerancia.create(scheduleId, command.name(), command.type(), command.minutes(), command.description());
        var saved = toleranciaRepository.save(tolerancia);
        return toResult(saved);
    }

    @Override
    public PaginatedResult<ToleranciaResult> findAllByScheduleId(Long scheduleId, int page, int size) {
        if (!scheduleRepository.findById(scheduleId).isPresent()) {
            throw new IllegalArgumentException("Schedule no encontrado: " + scheduleId);
        }
        var tolerancias = toleranciaRepository.findAllByScheduleId(scheduleId, page, size);
        var total = toleranciaRepository.countByScheduleId(scheduleId);
        var results = tolerancias.stream().map(this::toResult).toList();
        var totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PaginatedResult<>(results, page, size, total, totalPages);
    }

    @Override
    public ToleranciaResult patch(Long scheduleId, Long toleranciaId, PatchToleranciaCommand command) {
        var tolerancia = toleranciaRepository.findByScheduleIdAndId(scheduleId, toleranciaId)
                .orElseThrow(() -> new IllegalArgumentException("Tolerancia no encontrada: " + toleranciaId));
        if (command.name() != null) {
            tolerancia = Tolerancia.restore(
                    tolerancia.getId(), tolerancia.getScheduleId(), command.name(),
                    tolerancia.getType(), tolerancia.getMinutes(),
                    tolerancia.getDescription(), tolerancia.isActivo(),
                    tolerancia.getCreatedAt(), tolerancia.getUpdatedAt()
            );
        }
        if (command.minutes() != null) {
            tolerancia.updateMinutes(command.minutes());
        }
        if (command.description() != null) {
            tolerancia = Tolerancia.restore(
                    tolerancia.getId(), tolerancia.getScheduleId(), tolerancia.getName(),
                    tolerancia.getType(), tolerancia.getMinutes(),
                    command.description(), tolerancia.isActivo(),
                    tolerancia.getCreatedAt(), tolerancia.getUpdatedAt()
            );
        }
        if (command.activo() != null) {
            if (command.activo()) {
                tolerancia.activate();
            } else {
                tolerancia.deactivate();
            }
        }
        var saved = toleranciaRepository.save(tolerancia);
        return toResult(saved);
    }

    @Override
    public void deleteById(Long scheduleId, Long toleranciaId) {
        var tolerancia = toleranciaRepository.findByScheduleIdAndId(scheduleId, toleranciaId)
                .orElseThrow(() -> new IllegalArgumentException("Tolerancia no encontrada: " + toleranciaId));
        toleranciaRepository.deleteById(tolerancia.getId());
    }

    private ToleranciaResult toResult(Tolerancia tolerancia) {
        return new ToleranciaResult(
                tolerancia.getId(),
                tolerancia.getScheduleId(),
                tolerancia.getName(),
                tolerancia.getType(),
                tolerancia.getMinutes(),
                tolerancia.getDescription(),
                tolerancia.isActivo(),
                tolerancia.getCreatedAt(),
                tolerancia.getUpdatedAt()
        );
    }
}
