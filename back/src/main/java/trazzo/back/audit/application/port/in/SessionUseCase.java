package trazzo.back.audit.application.port.in;

import trazzo.back.audit.application.dto.result.PaginatedResult;
import trazzo.back.audit.application.dto.result.SessionResult;
import trazzo.back.audit.domain.model.tenant.SessionState;

public interface SessionUseCase {
    PaginatedResult<SessionResult> findAll(String tenantUserId, SessionState state,
        String ipAddress, int page, int size, String sort);
    SessionResult findById(Long id);
}
