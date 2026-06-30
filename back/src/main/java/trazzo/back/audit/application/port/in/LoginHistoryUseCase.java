package trazzo.back.audit.application.port.in;

import trazzo.back.audit.application.dto.result.LogInHistoryResult;
import trazzo.back.audit.application.dto.result.PaginatedResult;
import trazzo.back.audit.domain.model.master.StatusLogin;

public interface LoginHistoryUseCase {
    PaginatedResult<LogInHistoryResult> findAll(String userId, String attemptedEmail,
        StatusLogin status, String fechaDesde, String fechaHasta, int page, int size, String sort);
    LogInHistoryResult findById(Long id);
}
