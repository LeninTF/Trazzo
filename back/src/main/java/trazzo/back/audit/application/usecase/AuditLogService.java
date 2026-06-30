package trazzo.back.audit.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.audit.application.dto.result.AuditLogDetailResult;
import trazzo.back.audit.application.dto.result.AuditLogResult;
import trazzo.back.audit.application.dto.result.PaginatedResult;
import trazzo.back.audit.application.port.in.AuditLogUseCase;
import trazzo.back.audit.application.port.out.AuditRepositoryPort;
import trazzo.back.audit.application.port.out.TenantInfoPort;
import trazzo.back.audit.application.port.out.UserInfoPort;
import trazzo.back.audit.domain.exception.AuditNotFoundException;
import trazzo.back.audit.domain.model.master.Action;
import trazzo.back.audit.domain.model.master.Audit;
import trazzo.back.audit.infrastructure.adapters.out.persistence.adapter.SortUtils;
import trazzo.back.audit.application.port.out.UserInfoPort.UserInfo;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

@RequiredArgsConstructor
public class AuditLogService implements AuditLogUseCase {

    private final AuditRepositoryPort auditRepository;
    private final UserInfoPort userInfoPort;
    private final TenantInfoPort tenantInfoPort;

    @Override
    public PaginatedResult<AuditLogResult> findAll(String searchTerm, String tenantId, Action action,
            String entity, String fechaDesde, String fechaHasta, int page, int size, String sort) {
        LocalDateTime desde = fechaDesde != null ? LocalDate.parse(fechaDesde).atStartOfDay() : null;
        LocalDateTime hasta = fechaHasta != null ? LocalDate.parse(fechaHasta).atTime(LocalTime.MAX) : null;
        var pageable = PageRequest.of(page, size, SortUtils.parseSort(sort, f -> f));
        var audits = auditRepository.findAll(searchTerm, action, entity, desde, hasta, pageable);
        var total = auditRepository.count(searchTerm, action, entity, desde, hasta);
        var totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        var results = audits.stream().map(this::toResult).toList();
        return new PaginatedResult<>(results, page, size, total, totalPages);
    }

    @Override
    public AuditLogDetailResult findById(Long id) {
        return auditRepository.findById(id)
                .map(this::toDetailResult)
                .orElseThrow(() -> new AuditNotFoundException("Audit not found: " + id));
    }

    private AuditLogResult toResult(Audit audit) {
        String userId = audit.getUserId();
        Optional<TenantInfoPort.TenantInfo> tenantInfo = tenantInfoPort.findByTenantId(userId);
        Optional<UserInfoPort.UserInfo> userInfo = userId != null ? userInfoPort.findByUserId(userId) : Optional.empty();
        String eventId = "EVT-" + String.format("%05d", audit.getId() != null ? audit.getId() : 0) + "-X";
        String tipo = switch (audit.getAction()) {
            case DELETE -> "advertencia";
            default -> "exito";
        };
        return new AuditLogResult(
                audit.getId(), eventId, audit.getCreatedAt(),
                tenantInfo.map(t -> t.tenantName()).orElse(null),
                tenantInfo.map(t -> t.tenantId()).orElse(null),
                userInfo.map(u -> u.userName()).orElse(null),
                userInfo.map(u -> u.userEmail()).orElse(null),
                audit.getAction().name(), tipo,
                audit.getEntity(), audit.getEntityId(),
                audit.getIpAdress(), audit.getUserAgent(),
                audit.getPreviousValue(), audit.getNewValue()
        );
    }

    private AuditLogDetailResult toDetailResult(Audit audit) {
        return new AuditLogDetailResult(
                audit.getId(), audit.getEntity(), audit.getEntityId(), audit.getAction(),
                audit.getUserId(), audit.getEndpoint(), audit.getIpAdress(),
                audit.getUserAgent(), audit.getPreviousValue(), audit.getNewValue(),
                audit.getCreatedAt()
        );
    }
}
