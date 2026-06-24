package trazzo.back.saasglobal.application.port.out;

import trazzo.back.saasglobal.infrastructure.adapters.out.persistence.entity.RefreshTokenEntity;

import java.util.Optional;

public interface RefreshTokenRepositoryPort {
    RefreshTokenEntity save(RefreshTokenEntity token);
    Optional<RefreshTokenEntity> findByToken(String token);
    void revokeAllByUserId(Long userId);
    void revokeByToken(String token);
}
