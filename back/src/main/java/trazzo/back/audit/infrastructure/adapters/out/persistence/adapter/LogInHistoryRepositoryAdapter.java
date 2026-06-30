package trazzo.back.audit.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.audit.application.port.out.LogInHistoryRepositoryPort;
import trazzo.back.audit.domain.model.master.LogInHistory;
import trazzo.back.audit.domain.model.master.StatusLogin;
import trazzo.back.audit.infrastructure.adapters.out.persistence.mapper.LogInHistoryMapper;
import trazzo.back.audit.infrastructure.adapters.out.persistence.repository.LogInHistoryJpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LogInHistoryRepositoryAdapter implements LogInHistoryRepositoryPort {

    private final LogInHistoryJpaRepository jpaRepository;

    @Override
    public List<LogInHistory> findAll(String userId, String attemptedEmail, StatusLogin status,
            LocalDateTime fechaDesde, LocalDateTime fechaHasta, Pageable pageable) {
        UUID userIdUuid = userId != null ? UUID.fromString(userId) : null;
        return jpaRepository.findByFilters(userIdUuid, attemptedEmail, status, fechaDesde, fechaHasta, pageable)
                .stream()
                .map(LogInHistoryMapper::toDomain)
                .toList();
    }

    @Override
    public long count(String userId, String attemptedEmail, StatusLogin status,
            LocalDateTime fechaDesde, LocalDateTime fechaHasta) {
        UUID userIdUuid = userId != null ? UUID.fromString(userId) : null;
        return jpaRepository.countByFilters(userIdUuid, attemptedEmail, status, fechaDesde, fechaHasta);
    }

    @Override
    public Optional<LogInHistory> findById(String id) {
        return jpaRepository.findById(UUID.fromString(id))
                .map(LogInHistoryMapper::toDomain);
    }
}
