package trazzo.back.organization.application.port.out;

import trazzo.back.organization.domain.model.business.Department;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepositoryPort {
    Department save(Department department);
    Optional<Department> findById(Long id);
    List<Department> findAll(Long areaId, Boolean state, String search, int page, int size, String sort);
    long count(Long areaId, Boolean state, String search);
    boolean existsByAreaIdAndName(Long areaId, String name);
    boolean existsByAreaIdAndNameAndIdNot(Long areaId, String name, Long id);
}
