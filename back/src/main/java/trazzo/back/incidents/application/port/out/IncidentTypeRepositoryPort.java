package trazzo.back.incidents.application.port.out;

import trazzo.back.incidents.domain.model.IncidentType;

import java.util.List;
import java.util.Optional;

public interface IncidentTypeRepositoryPort {
    IncidentType save(IncidentType type);
    Optional<IncidentType> findById(String id);
    List<IncidentType> findByIdIn(List<String> ids);
    List<IncidentType> findAll(Boolean activo, int page, int size);
    long count(Boolean activo);
    boolean existsByNombre(String nombre);
}
