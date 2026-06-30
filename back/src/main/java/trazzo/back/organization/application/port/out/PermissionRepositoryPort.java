package trazzo.back.organization.application.port.out;

import trazzo.back.organization.domain.model.roles.Permissions;

import java.util.List;
import java.util.Optional;

public interface PermissionRepositoryPort {
    Permissions save(Permissions permission);
    Optional<Permissions> findById(String id);
    List<Permissions> findAll(String search, int page, int size, String sort);
    long count(String search);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, String id);
    void deleteById(String id);
}
