package trazzo.back.corehr.application.port.out;

import trazzo.back.corehr.domain.model.schedule.Shift;

import java.util.List;
import java.util.Optional;

public interface ShiftRepositoryPort {
    Shift save(Shift shift);
    Optional<Shift> findById(Long id);
    List<Shift> findAll(String search, int page, int size, String sort);
    long count(String search);
    boolean existsByName(String name);
    void deleteById(Long id);
}
