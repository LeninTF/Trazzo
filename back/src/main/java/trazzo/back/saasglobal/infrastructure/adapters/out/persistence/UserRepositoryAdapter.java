package trazzo.back.saasglobal.infrastructure.adapters.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import trazzo.back.saasglobal.application.port.out.UserRepositoryPort;
import trazzo.back.saasglobal.infrastructure.adapters.out.persistence.entity.UserEntity;
import trazzo.back.saasglobal.infrastructure.adapters.out.persistence.repository.UserSpringDataRepo;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserSpringDataRepo springDataRepo;

    @Override
    public Optional<UserEntity> findByEmail(String email) {
        return springDataRepo.findByEmail(email);
    }

    @Override
    public boolean existsByEmail(String email) {
        return springDataRepo.existsByEmail(email);
    }

    @Override
    public UserEntity save(UserEntity user) {
        return springDataRepo.save(user);
    }
}
