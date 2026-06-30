package trazzo.back.audit.infrastructure.adapters.out.persistence.mapper;

import trazzo.back.audit.domain.model.master.LogInHistory;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.LogInHistoryEntity;

import java.util.UUID;

public final class LogInHistoryMapper {

    private LogInHistoryMapper() {
    }

    public static LogInHistoryEntity toEntity(LogInHistory domain) {
        var entity = new LogInHistoryEntity();
        if (domain.getId() != null) {
            entity.setId(UUID.fromString(domain.getId()));
        } else {
            entity.setId(UUID.randomUUID());
        }
        entity.setUserId(domain.getUserId() != null ? UUID.fromString(domain.getUserId()) : null);
        entity.setAttemptedEmail(domain.getAttemptedEmail());
        entity.setStatus(domain.getStatus());
        entity.setIpAddress(domain.getIpAddress());
        entity.setUserAgent(domain.getUserAgent());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    public static LogInHistory toDomain(LogInHistoryEntity entity) {
        return new LogInHistory(
                entity.getId() != null ? entity.getId().toString() : null,
                entity.getUserId() != null ? entity.getUserId().toString() : null,
                entity.getAttemptedEmail(),
                entity.getStatus(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                entity.getCreatedAt()
        );
    }
}
