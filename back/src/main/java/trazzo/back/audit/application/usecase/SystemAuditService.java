package trazzo.back.audit.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.audit.application.dto.result.PaginatedResult;
import trazzo.back.audit.application.dto.result.SystemAuditResult;
import trazzo.back.audit.application.port.in.SystemAuditUseCase;
import trazzo.back.audit.application.port.out.SystemAuditRepositoryPort;
import trazzo.back.audit.domain.exception.AuditNotFoundException;
import trazzo.back.audit.domain.model.tenant.SystemAudit;
import trazzo.back.audit.infrastructure.adapters.out.persistence.adapter.SortUtils;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@RequiredArgsConstructor
public class SystemAuditService implements SystemAuditUseCase {

    private final SystemAuditRepositoryPort systemAuditRepository;

    @Override
    public PaginatedResult<SystemAuditResult> findAll(String searchTerm, String module,
            String entity, String fechaDesde, String fechaHasta, int page, int size, String sort) {
        LocalDateTime desde = fechaDesde != null ? LocalDate.parse(fechaDesde).atStartOfDay() : null;
        LocalDateTime hasta = fechaHasta != null ? LocalDate.parse(fechaHasta).atTime(LocalTime.MAX) : null;
        var pageable = PageRequest.of(page, size, SortUtils.parseSort(sort, f -> f, "date"));
        var audits = systemAuditRepository.findAll(searchTerm, module, entity, desde, hasta, pageable);
        var total = systemAuditRepository.count(searchTerm, module, entity, desde, hasta);
        var totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        var results = audits.stream().map(this::toResult).toList();
        return new PaginatedResult<>(results, page, size, total, totalPages);
    }

    @Override
    public SystemAuditResult findById(Long id) {
        return systemAuditRepository.findById(id)
                .map(this::toResult)
                .orElseThrow(() -> new AuditNotFoundException("System audit not found: " + id));
    }

    private SystemAuditResult toResult(SystemAudit audit) {
        return new SystemAuditResult(
                audit.getId(), audit.getUserTenantId(), audit.getSystemActions(),
                audit.getModule(), audit.getEntity(), audit.getEntityId(),
                audit.getHttpMethod(), audit.getEndpoint(), audit.getDescription(),
                audit.getPreviousValue(), audit.getNewValue(),
                audit.getIpAddress(), audit.getResult(), audit.getCreatedAt()
        );
    }
}
