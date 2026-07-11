package trazzo.back.organization.application.port.out;

import trazzo.back.organization.domain.model.roles.Role;

import java.util.List;
import java.util.Optional;

public interface RoleRepositoryPort {
    Role save(Role role);
    Optional<Role> findById(String id);
    List<Role> findAll(String search, int page, int size, String sort);
    long count(String search);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, String id);
    void deleteById(String id);
}
