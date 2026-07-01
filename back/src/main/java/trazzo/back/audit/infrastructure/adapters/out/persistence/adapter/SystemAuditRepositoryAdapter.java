package trazzo.back.audit.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.audit.application.port.out.SystemAuditRepositoryPort;
import trazzo.back.audit.domain.model.tenant.SystemAudit;
import trazzo.back.audit.infrastructure.adapters.out.persistence.mapper.SystemAuditMapper;
import trazzo.back.audit.infrastructure.adapters.out.persistence.repository.SystemAuditJpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SystemAuditRepositoryAdapter implements SystemAuditRepositoryPort {

    private final SystemAuditJpaRepository jpaRepository;

    @Override
    public List<SystemAudit> findAll(String searchTerm, String module, String entity,
            LocalDateTime fechaDesde, LocalDateTime fechaHasta, Pageable pageable) {
        return jpaRepository.findByFilters(searchTerm, module, fechaDesde, fechaHasta, pageable)
                .stream()
                .map(SystemAuditMapper::toDomain)
                .toList();
    }

    @Override
    public long count(String searchTerm, String module, String entity,
            LocalDateTime fechaDesde, LocalDateTime fechaHasta) {
        return jpaRepository.countByFilters(searchTerm, module, fechaDesde, fechaHasta);
    }

    @Override
    public Optional<SystemAudit> findById(Long id) {
        return jpaRepository.findById(id)
                .map(SystemAuditMapper::toDomain);
    }
}
