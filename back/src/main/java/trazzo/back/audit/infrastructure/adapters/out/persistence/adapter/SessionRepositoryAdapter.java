package trazzo.back.audit.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.audit.application.port.out.SessionRepositoryPort;
import trazzo.back.audit.domain.model.tenant.Session;
import trazzo.back.audit.domain.model.tenant.SessionState;
import trazzo.back.audit.infrastructure.adapters.out.persistence.mapper.SessionMapper;
import trazzo.back.audit.infrastructure.adapters.out.persistence.repository.SessionJpaRepository;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionRepositoryAdapter implements SessionRepositoryPort {

    private final SessionJpaRepository jpaRepository;
    private final Clock clock;

    @Override
    public List<Session> findAll(String tenantUserId, SessionState state, String ipAddress, Pageable pageable) {
        Boolean stateBool = state != null ? state == SessionState.ACTIVE : null;
        return jpaRepository.findByFilters(tenantUserId, stateBool, ipAddress, pageable)
                .stream()
                .map(e -> SessionMapper.toDomain(e, clock))
                .toList();
    }

    @Override
    public long count(String tenantUserId, SessionState state, String ipAddress) {
        Boolean stateBool = state != null ? state == SessionState.ACTIVE : null;
        return jpaRepository.countByFilters(tenantUserId, stateBool, ipAddress);
    }

    @Override
    public Optional<Session> findById(Long id) {
        return jpaRepository.findById(id)
                .map(e -> SessionMapper.toDomain(e, clock));
    }
}
