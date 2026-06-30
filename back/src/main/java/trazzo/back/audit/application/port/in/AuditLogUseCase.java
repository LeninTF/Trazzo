package trazzo.back.audit.application.port.in;

import trazzo.back.audit.application.dto.result.AuditLogDetailResult;
import trazzo.back.audit.application.dto.result.AuditLogResult;
import trazzo.back.audit.application.dto.result.PaginatedResult;
import trazzo.back.audit.domain.model.master.Action;

public interface AuditLogUseCase {
    PaginatedResult<AuditLogResult> findAll(String searchTerm, String tenantId, Action action,
        String entity, String fechaDesde, String fechaHasta, int page, int size, String sort);
    AuditLogDetailResult findById(Long id);
}
