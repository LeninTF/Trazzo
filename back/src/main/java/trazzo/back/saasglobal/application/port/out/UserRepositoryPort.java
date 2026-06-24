package trazzo.back.saasglobal.application.port.out;

import trazzo.back.saasglobal.infrastructure.adapters.out.persistence.entity.UserEntity;

import java.util.Optional;

public interface UserRepositoryPort {
    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);
    UserEntity save(UserEntity user);
}
