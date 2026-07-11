package trazzo.back.organization.application.port.out;

import trazzo.back.organization.domain.model.business.Area;

import java.util.List;
import java.util.Optional;

public interface AreaRepositoryPort {
    Area save(Area area);
    Optional<Area> findById(Long id);
    List<Area> findAll(Long branchId, Boolean state, String search, int page, int size, String sort);
    long count(Long branchId, Boolean state, String search);
    boolean existsByBranchIdAndName(Long branchId, String name);
    boolean existsByBranchIdAndNameAndIdNot(Long branchId, String name, Long id);
}
