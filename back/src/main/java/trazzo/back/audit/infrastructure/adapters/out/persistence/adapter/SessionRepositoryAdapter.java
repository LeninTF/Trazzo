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

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SessionRepositoryAdapter implements SessionRepositoryPort {

    private final SessionJpaRepository jpaRepository;

    @Override
    public List<Session> findAll(String tenantUserId, SessionState state, String ipAddress, Pageable pageable) {
        var allEntities = jpaRepository.findAll(pageable);
        return allEntities.stream()
                .map(SessionMapper::toDomain)
                .filter(s -> tenantUserId == null || tenantUserId.equals(s.getTenantUserId()))
                .filter(s -> state == null || state == s.getState())
                .filter(s -> ipAddress == null || ipAddress.equals(s.getIpAddress()))
                .toList();
    }

    @Override
    public long count(String tenantUserId, SessionState state, String ipAddress) {
        var allEntities = jpaRepository.findAll();
        return allEntities.stream()
                .map(SessionMapper::toDomain)
                .filter(s -> tenantUserId == null || tenantUserId.equals(s.getTenantUserId()))
                .filter(s -> state == null || state == s.getState())
                .filter(s -> ipAddress == null || ipAddress.equals(s.getIpAddress()))
                .count();
    }

    @Override
    public Optional<Session> findById(Long id) {
        return jpaRepository.findById(id)
                .map(SessionMapper::toDomain);
    }
}
