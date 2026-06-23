package trazzo.back.saasglobal.infrastructure.adapters.out.persistence.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import trazzo.back.saasglobal.domain.model.iam.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT u FROM User u JOIN FETCH u.tenant LEFT JOIN FETCH u.roleMaster WHERE u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);
}
