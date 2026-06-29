package trazzo.back.corehr.application.port.out;

import trazzo.back.corehr.domain.model.ToleranciaType;
import trazzo.back.corehr.domain.model.schedule.Tolerancia;

import java.util.List;
import java.util.Optional;

public interface ToleranciaRepositoryPort {
    Tolerancia save(Tolerancia tolerancia);
    Optional<Tolerancia> findById(Long id);
    List<Tolerancia> findAllByScheduleId(Long scheduleId, int page, int size);
    long countByScheduleId(Long scheduleId);
    boolean existsActiveByScheduleIdAndType(Long scheduleId, ToleranciaType type);
    void deleteById(Long id);
    Optional<Tolerancia> findByScheduleIdAndId(Long scheduleId, Long toleranciaId);
}
