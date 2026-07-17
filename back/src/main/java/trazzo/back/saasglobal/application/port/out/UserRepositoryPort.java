package trazzo.back.saasglobal.application.port.out;

import java.util.List;
import java.util.Optional;
import trazzo.back.saasglobal.domain.model.iam.User;

public interface UserRepositoryPort {
    Optional<User> findByEmail(String email);
    Optional<User> findById(String id);
    Optional<User> findByTenantId(String tenantId);
    List<User> findAll(String search, int page, int size);
    long countAll(String search);
    User save(User user);
    User update(User user);
}
