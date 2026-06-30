package trazzo.back.audit.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.audit.application.dto.result.PaginatedResult;
import trazzo.back.audit.application.dto.result.SessionResult;
import trazzo.back.audit.application.port.in.SessionUseCase;
import trazzo.back.audit.application.port.out.SessionRepositoryPort;
import trazzo.back.audit.domain.exception.AuditNotFoundException;
import trazzo.back.audit.domain.model.tenant.Session;
import trazzo.back.audit.domain.model.tenant.SessionState;
import trazzo.back.audit.infrastructure.adapters.out.persistence.adapter.SortUtils;
import org.springframework.data.domain.PageRequest;

@RequiredArgsConstructor
public class SessionService implements SessionUseCase {

    private final SessionRepositoryPort sessionRepository;

    @Override
    public PaginatedResult<SessionResult> findAll(String tenantUserId, SessionState state,
            String ipAddress, int page, int size, String sort) {
        var pageable = PageRequest.of(page, size, SortUtils.parseSort(sort, f -> f));
        var sessions = sessionRepository.findAll(tenantUserId, state, ipAddress, pageable);
        var total = sessionRepository.count(tenantUserId, state, ipAddress);
        var totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        var results = sessions.stream().map(this::toResult).toList();
        return new PaginatedResult<>(results, page, size, total, totalPages);
    }

    @Override
    public SessionResult findById(Long id) {
        return sessionRepository.findById(id)
                .map(this::toResult)
                .orElseThrow(() -> new AuditNotFoundException("Session not found: " + id));
    }

    private SessionResult toResult(Session session) {
        return new SessionResult(
                session.getId(), session.getTenantUserId(), session.getRefreshTokenHash(),
                session.getIpAddress(), session.getUserAgent(), session.getDeviceFingerprint(),
                session.getLoginAt(), session.getLasActivityAt(), session.getLogoutAt(),
                session.getExpiresAt(), session.getState(),
                session.getCreatedAt(), session.getUpdatedAt()
        );
    }
}
