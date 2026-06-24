package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.saasglobal.application.port.out.RefreshTokenRepositoryPort;
import trazzo.back.saasglobal.infrastructure.adapters.out.persistence.entity.RefreshTokenEntity;
import trazzo.back.saasglobal.infrastructure.adapters.out.persistence.repository.RefreshTokenSpringDataRepo;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenSpringDataRepo springDataRepo;

    @Override
    public RefreshTokenEntity save(RefreshTokenEntity token) {
        return springDataRepo.save(token);
    }

    @Override
    public Optional<RefreshTokenEntity> findByToken(String token) {
        return springDataRepo.findByToken(token);
    }

    @Override
    @Transactional
    public void revokeAllByUserId(Long userId) {
        springDataRepo.revokeAllByUserId(userId);
    }

    @Override
    @Transactional
    public void revokeByToken(String token) {
        springDataRepo.revokeByToken(token);
    }
}
