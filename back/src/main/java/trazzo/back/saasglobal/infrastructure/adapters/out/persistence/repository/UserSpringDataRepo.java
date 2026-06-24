package trazzo.back.saasglobal.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import trazzo.back.saasglobal.infrastructure.adapters.out.persistence.entity.UserEntity;

import java.util.Optional;

public interface UserSpringDataRepo extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    boolean existsByEmail(String email);
}
