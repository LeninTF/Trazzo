package trazzo.back.audit.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.audit.application.port.out.AuditRepositoryPort;
import trazzo.back.audit.domain.model.master.Action;
import trazzo.back.audit.domain.model.master.Audit;
import trazzo.back.audit.infrastructure.adapters.out.persistence.mapper.AuditMapper;
import trazzo.back.audit.infrastructure.adapters.out.persistence.repository.AuditJpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditRepositoryAdapter implements AuditRepositoryPort {

    private final AuditJpaRepository jpaRepository;

    @Override
    public List<Audit> findAll(String searchTerm, Action action, String entity,
            LocalDateTime fechaDesde, LocalDateTime fechaHasta, Pageable pageable) {
        return jpaRepository.findByFilters(searchTerm, action, entity, fechaDesde, fechaHasta, pageable)
                .stream()
                .map(AuditMapper::toDomain)
                .toList();
    }

    @Override
    public long count(String searchTerm, Action action, String entity,
            LocalDateTime fechaDesde, LocalDateTime fechaHasta) {
        return jpaRepository.findByFilters(searchTerm, action, entity, fechaDesde, fechaHasta, Pageable.unpaged())
                .getTotalElements();
    }

    @Override
    public Optional<Audit> findById(Long id) {
        return Optional.empty();
    }
}
