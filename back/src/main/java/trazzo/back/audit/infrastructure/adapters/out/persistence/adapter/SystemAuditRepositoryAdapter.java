package trazzo.back.audit.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.audit.application.port.out.SystemAuditRepositoryPort;
import trazzo.back.audit.domain.model.tenant.SystemAudit;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.SystemAuditEntity;
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
        var allEntities = jpaRepository.findAll(pageable);
        return allEntities.stream()
                .map(SystemAuditMapper::toDomain)
                .filter(a -> searchTerm == null || searchTerm.isBlank()
                        || matchesSearch(a, searchTerm))
                .filter(a -> module == null || module.equals(a.getModule()))
                .filter(a -> entity == null || entity.equals(a.getEntity()))
                .filter(a -> fechaDesde == null || a.getCreatedAt() != null && !a.getCreatedAt().isBefore(fechaDesde))
                .filter(a -> fechaHasta == null || a.getCreatedAt() != null && !a.getCreatedAt().isAfter(fechaHasta))
                .toList();
    }

    @Override
    public long count(String searchTerm, String module, String entity,
            LocalDateTime fechaDesde, LocalDateTime fechaHasta) {
        var allEntities = jpaRepository.findAll();
        return allEntities.stream()
                .map(SystemAuditMapper::toDomain)
                .filter(a -> searchTerm == null || searchTerm.isBlank()
                        || matchesSearch(a, searchTerm))
                .filter(a -> module == null || module.equals(a.getModule()))
                .filter(a -> entity == null || entity.equals(a.getEntity()))
                .filter(a -> fechaDesde == null || a.getCreatedAt() != null && !a.getCreatedAt().isBefore(fechaDesde))
                .filter(a -> fechaHasta == null || a.getCreatedAt() != null && !a.getCreatedAt().isAfter(fechaHasta))
                .count();
    }

    @Override
    public Optional<SystemAudit> findById(Long id) {
        return jpaRepository.findById(id)
                .map(SystemAuditMapper::toDomain);
    }

    private boolean matchesSearch(SystemAudit audit, String searchTerm) {
        String term = searchTerm.toLowerCase();
        return (audit.getDescription() != null && audit.getDescription().toLowerCase().contains(term))
                || (audit.getModule() != null && audit.getModule().toLowerCase().contains(term))
                || (audit.getEndpoint() != null && audit.getEndpoint().toLowerCase().contains(term))
                || (audit.getIpAddress() != null && audit.getIpAddress().toLowerCase().contains(term));
    }
}
