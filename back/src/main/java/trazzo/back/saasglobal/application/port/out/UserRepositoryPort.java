package trazzo.back.saasglobal.application.port.out;

import java.util.Optional;
import trazzo.back.saasglobal.domain.model.iam.User;

public interface UserRepositoryPort {
    Optional<User> findByEmail(String email);
    User save(User user);
}
