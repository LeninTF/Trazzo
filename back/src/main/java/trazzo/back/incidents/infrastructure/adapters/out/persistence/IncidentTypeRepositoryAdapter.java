package trazzo.back.incidents.infrastructure.adapters.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import trazzo.back.incidents.application.port.out.IncidentTypeRepositoryPort;
import trazzo.back.incidents.domain.model.IncidentType;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.mapper.IncidentTypeMapper;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.repository.IncidentTypeSpringDataRepository;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class IncidentTypeRepositoryAdapter implements IncidentTypeRepositoryPort {

    private final IncidentTypeSpringDataRepository repository;

    @Override
    public IncidentType save(IncidentType type) {
        var entity = IncidentTypeMapper.toEntity(type);
        var saved = repository.save(entity);
        return IncidentTypeMapper.toDomain(saved);
    }

    @Override
    public Optional<IncidentType> findById(String id) {
        Integer intId = toInt(id);
        if (intId == null) return Optional.empty();
        return repository.findById(intId).map(IncidentTypeMapper::toDomain);
    }

    @Override
    public List<IncidentType> findByIdIn(List<String> ids) {
        var intIds = ids.stream()
                .map(IncidentTypeRepositoryAdapter::toInt)
                .filter(java.util.Objects::nonNull)
                .toList();
        return repository.findByIdIn(intIds)
                .stream()
                .map(IncidentTypeMapper::toDomain)
                .toList();
    }

    @Override
    public List<IncidentType> findAll(Boolean activo, int page, int size) {
        var pageable = PageRequest.of(page, size);
        if (activo != null) {
            return repository.findByActivo(activo, pageable)
                    .stream()
                    .map(IncidentTypeMapper::toDomain)
                    .toList();
        }
        return repository.findAll(pageable)
                .stream()
                .map(IncidentTypeMapper::toDomain)
                .toList();
    }

    @Override
    public long count(Boolean activo) {
        if (activo != null) {
            return repository.findByActivo(activo, PageRequest.of(0, 1)).getTotalElements();
        }
        return repository.count();
    }

    @Override
    public boolean existsByNombre(String nombre) {
        return repository.existsByNombre(nombre);
    }

    private static Integer toInt(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
