package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.corehr.application.port.out.ToleranciaRepositoryPort;
import trazzo.back.corehr.domain.model.ToleranciaType;
import trazzo.back.corehr.domain.model.schedule.Tolerancia;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper.ToleranciaMapper;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.repository.ToleranciaJpaRepository;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ToleranciaRepositoryAdapter implements ToleranciaRepositoryPort {

    private final ToleranciaJpaRepository toleranciaRepo;

    @Override
    @Transactional
    public Tolerancia save(Tolerancia tolerancia) {
        var entity = ToleranciaMapper.toEntity(tolerancia);
        var saved = toleranciaRepo.save(entity);
        return ToleranciaMapper.toDomain(saved);
    }

    @Override
    public Optional<Tolerancia> findById(Long id) {
        return toleranciaRepo.findById(id).map(ToleranciaMapper::toDomain);
    }

    @Override
    public List<Tolerancia> findAllByScheduleId(Long scheduleId, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return toleranciaRepo.findByScheduleId(scheduleId, pageable)
                .stream()
                .map(ToleranciaMapper::toDomain)
                .toList();
    }

    @Override
    public long countByScheduleId(Long scheduleId) {
        return toleranciaRepo.countByScheduleId(scheduleId);
    }

    @Override
    public boolean existsActiveByScheduleIdAndType(Long scheduleId, ToleranciaType type) {
        return toleranciaRepo.existsByScheduleIdAndTypeAndActivoTrue(scheduleId, type);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        toleranciaRepo.deleteById(id);
    }

    @Override
    public Optional<Tolerancia> findByScheduleIdAndId(Long scheduleId, Long toleranciaId) {
        return toleranciaRepo.findByScheduleIdAndId(scheduleId, toleranciaId)
                .map(ToleranciaMapper::toDomain);
    }
}
