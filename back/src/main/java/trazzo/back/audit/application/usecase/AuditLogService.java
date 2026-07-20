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
import trazzo.back.shared.util.SortUtils;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        var audits = auditRepository.findAll(searchTerm, tenantId, action, entity, desde, hasta, pageable);
        var total = auditRepository.count(searchTerm, tenantId, action, entity, desde, hasta);
        var totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;

        List<String> userIds = audits.stream()
                .map(Audit::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, TenantInfoPort.TenantInfo> tenantMap = tenantInfoPort.findByUserIds(userIds);
        Map<String, UserInfoPort.UserInfo> userMap = userInfoPort.findByUserIds(userIds);

        var results = audits.stream()
                .map(audit -> toResult(audit, tenantMap, userMap))
                .toList();
        return new PaginatedResult<>(results, page, size, total, totalPages);
    }

    @Override
    public AuditLogDetailResult findById(String id) {
        return auditRepository.findById(id)
                .map(this::toDetailResult)
                .orElseThrow(() -> new AuditNotFoundException("Audit not found: " + id));
    }

    private AuditLogResult toResult(Audit audit,
            Map<String, TenantInfoPort.TenantInfo> tenantMap,
            Map<String, UserInfoPort.UserInfo> userMap) {
        String userId = audit.getUserId();
        TenantInfoPort.TenantInfo tenantInfo = userId != null ? tenantMap.get(userId) : null;
        UserInfoPort.UserInfo userInfo = userId != null ? userMap.get(userId) : null;
        String eventId = "EVT-" + (audit.getId() != null ? audit.getId() : "0");
        String tipo = switch (audit.getAction()) {
            case DELETE -> "advertencia";
            default -> "exito";
        };
        return new AuditLogResult(
                audit.getId(), eventId, audit.getCreatedAt(),
                tenantInfo != null ? tenantInfo.tenantName() : null,
                tenantInfo != null ? tenantInfo.tenantId() : null,
                userInfo != null ? userInfo.userName() : null,
                userInfo != null ? userInfo.userEmail() : null,
                audit.getAction().name(), tipo,
                audit.getEntity(), audit.getEntityId(),
                audit.getIpAddress(), audit.getUserAgent(),
                audit.getPreviousValue(), audit.getNewValue()
        );
    }

    private AuditLogDetailResult toDetailResult(Audit audit) {
        return new AuditLogDetailResult(
                audit.getId(), audit.getEntity(), audit.getEntityId(), audit.getAction(),
                audit.getUserId(), audit.getEndpoint(), audit.getIpAddress(),
                audit.getUserAgent(), audit.getPreviousValue(), audit.getNewValue(),
                audit.getCreatedAt()
        );
    }
}
