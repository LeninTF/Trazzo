package trazzo.back.audit.infrastructure.adapters.out.persistence.mapper;

import trazzo.back.audit.domain.model.tenant.Session;
import trazzo.back.audit.domain.model.tenant.SessionState;
import trazzo.back.audit.infrastructure.adapters.out.persistence.entity.SessionEntity;

public final class SessionMapper {

    private SessionMapper() {
    }

    public static SessionEntity toEntity(Session domain) {
        var entity = new SessionEntity();
        entity.setId(domain.getId());
        entity.setTenantUserId(domain.getTenantUserId());
        entity.setRefreshTokenHash(domain.getRefreshTokenHash());
        entity.setIpAddress(domain.getIpAddress());
        entity.setUserAgent(domain.getUserAgent());
        entity.setDeviceFingerprint(domain.getDeviceFingerprint());
        entity.setLoginAt(domain.getLoginAt());
        entity.setLastActivityAt(domain.getLasActivityAt());
        entity.setLogoutAt(domain.getLogoutAt());
        entity.setExpiresAt(domain.getExpiresAt());
        entity.setState(domain.getState());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public static Session toDomain(SessionEntity entity) {
        return new Session(
                entity.getId(),
                entity.getTenantUserId(),
                entity.getRefreshTokenHash(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                entity.getDeviceFingerprint(),
                entity.getLoginAt(),
                entity.getLastActivityAt(),
                entity.getLogoutAt(),
                entity.getExpiresAt(),
                entity.getState(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
