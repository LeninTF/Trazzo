package trazzo.back.incidents.application.port.out;

import trazzo.back.incidents.domain.model.Incident;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IncidentRepositoryPort {
    Incident save(Incident incident);
    Optional<Incident> findById(String id);
    List<Incident> findAll(String tenantUserId, String state, String tipoId,
                           LocalDateTime desde, LocalDateTime hasta, String search,
                           int page, int size, String sort);
    long count(String tenantUserId, String state, String tipoId,
               LocalDateTime desde, LocalDateTime hasta, String search);
    void deleteById(String id);
}
