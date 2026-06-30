package trazzo.back.audit.application.port.in;

import trazzo.back.audit.application.dto.result.PaginatedResult;
import trazzo.back.audit.application.dto.result.SystemAuditResult;

public interface SystemAuditUseCase {
    PaginatedResult<SystemAuditResult> findAll(String searchTerm, String module,
        String entity, String fechaDesde, String fechaHasta, int page, int size, String sort);
    SystemAuditResult findById(Long id);
}
