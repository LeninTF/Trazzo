package trazzo.back.saasglobal.application.usecase;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import trazzo.back.saasglobal.application.port.out.RefreshTokenRepositoryPort;
import trazzo.back.saasglobal.infrastructure.adapters.out.persistence.entity.RefreshTokenEntity;
import trazzo.back.saasglobal.infrastructure.adapters.out.persistence.entity.UserEntity;
import trazzo.back.shared.security.config.JwtProperties;

import java.time.Instant;
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

    public RefreshTokenEntity validate(String rawToken) {
        RefreshTokenEntity token = refreshTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token no encontrado"));

        if (token.isRevoked()) {
            throw new IllegalArgumentException("Refresh token revocado");
        }
        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.revokeByToken(rawToken);
            throw new IllegalArgumentException("Refresh token expirado");
        }
        return token;
    }

    public void revokeByToken(String rawToken) {
        refreshTokenRepository.revokeByToken(rawToken);
    }

    public void revokeAllByUser(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}
