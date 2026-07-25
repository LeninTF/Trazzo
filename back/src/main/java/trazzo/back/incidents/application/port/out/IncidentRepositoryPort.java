package trazzo.back.incidents.application.port.out;

import trazzo.back.incidents.domain.model.Incident;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IncidentRepositoryPort {
    Incident save(Incident incident);
    Optional<Incident> findById(Integer id);
    List<Incident> findAll(Integer tenantUserId, String state, Integer tipoId,
                           LocalDateTime desde, LocalDateTime hasta, String search,
                           int page, int size, String sort);
    long count(Integer tenantUserId, String state, Integer tipoId,
               LocalDateTime desde, LocalDateTime hasta, String search);
    void deleteById(Integer id);
}
