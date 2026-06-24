package trazzo.back.saasglobal.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import trazzo.back.saasglobal.application.port.out.RefreshTokenRepositoryPort;
import trazzo.back.saasglobal.infrastructure.adapters.out.persistence.entity.RefreshTokenEntity;
import trazzo.back.saasglobal.infrastructure.adapters.out.persistence.entity.UserEntity;
import trazzo.back.shared.security.config.JwtProperties;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public RefreshTokenEntity create(UserEntity user) {
        RefreshTokenEntity token = RefreshTokenEntity.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiryDate(Instant.now().plusMillis(jwtProperties.refreshExpirationMs()))
                .build();
        return refreshTokenRepository.save(token);
    }

    public Optional<RefreshTokenEntity> findByToken(String rawToken) {
        return refreshTokenRepository.findByToken(rawToken);
    }

    /**
     * Atomic CAS revocation: returns 1 if the token was active and is now revoked, 0 if already used.
     * Call this before validating to prevent concurrent replay attacks.
     */
    public int revokeAtomically(String rawToken) {
        return refreshTokenRepository.revokeByTokenIfActive(rawToken);
    }

    public void revokeByToken(String rawToken) {
        refreshTokenRepository.revokeByToken(rawToken);
    }

    public void revokeAllByUser(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}
