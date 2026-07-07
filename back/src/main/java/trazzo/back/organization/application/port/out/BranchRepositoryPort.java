package trazzo.back.organization.application.port.out;

import trazzo.back.organization.domain.model.business.Branch;

import java.util.List;
import java.util.Optional;

public interface BranchRepositoryPort {
    Branch save(Branch branch);
    Optional<Branch> findById(Long id);
    List<Branch> findAll(Boolean state, String search, int page, int size, String sort);
    long count(Boolean state, String search);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
}
