package trazzo.back.incidents.infrastructure.adapters.out.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import trazzo.back.incidents.application.port.out.IncidentRepositoryPort;
import trazzo.back.incidents.domain.model.Incident;
import trazzo.back.incidents.domain.model.IncidentState;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentEntity;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentPermissionEntity;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.mapper.IncidentMapper;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.repository.IncidentEvidenceSpringDataRepository;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.repository.IncidentPermissionSpringDataRepository;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.repository.IncidentSpringDataRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class IncidentRepositoryAdapter implements IncidentRepositoryPort {

    private final IncidentSpringDataRepository incidentRepo;
    private final IncidentEvidenceSpringDataRepository evidenceRepo;
    private final IncidentPermissionSpringDataRepository permissionRepo;

    @Override
    public Incident save(Incident incident) {
        var entity = IncidentMapper.toEntity(incident);
        var saved = incidentRepo.save(entity);
        if (entity.getPermission() != null) {
            entity.getPermission().setIncidentId(saved.getId());
            permissionRepo.save(entity.getPermission());
        }
        return findById(saved.getId()).orElseThrow();
    }

    @Override
    public Optional<Incident> findById(String id) {
        return incidentRepo.findById(id).map(entity -> {
            var permission = permissionRepo.findByIncidentId(id).orElse(null);
            entity.setPermission(permission);
            return IncidentMapper.toDomain(entity);
        });
    }

    @Override
    public List<Incident> findAll(String tenantUserId, String state, String tipoId,
                                   LocalDateTime desde, LocalDateTime hasta, String search,
                                   int page, int size, String sort) {
        var sortObj = parseSort(sort);
        var pageable = PageRequest.of(page, size, sortObj);
        var incidentState = parseState(state);
        Page<IncidentEntity> result;

        if (hasAnyFilter(tenantUserId, state, tipoId, desde, hasta, search)) {
            result = incidentRepo.findByFilters(tenantUserId, incidentState, tipoId, desde, hasta, search, pageable);
        } else {
            result = incidentRepo.findAll(pageable);
        }

        var incidentIds = result.stream()
                .map(IncidentEntity::getId)
                .toList();
        Map<String, IncidentPermissionEntity> permissionByIncidentId = permissionRepo.findByIncidentIdIn(incidentIds)
                .stream()
                .collect(Collectors.toMap(IncidentPermissionEntity::getIncidentId, p -> p));

        result.forEach(entity -> {
            var perm = permissionByIncidentId.get(entity.getId());
            entity.setPermission(perm);
        });

        return result.stream()
                .map(IncidentMapper::toDomain)
                .toList();
    }

    @Override
    public long count(String tenantUserId, String state, String tipoId,
                       LocalDateTime desde, LocalDateTime hasta, String search) {
        var incidentState = parseState(state);
        if (hasAnyFilter(tenantUserId, state, tipoId, desde, hasta, search)) {
            return incidentRepo.findByFilters(tenantUserId, incidentState, tipoId, desde, hasta, search,
                    PageRequest.of(0, 1)).getTotalElements();
        }
        return incidentRepo.count();
    }

    @Override
    public void deleteById(String id) {
        evidenceRepo.deleteByIncidentId(id);
        permissionRepo.deleteByIncidentId(id);
        incidentRepo.deleteById(id);
    }

    private static IncidentState parseState(String state) {
        if (state == null || state.isBlank()) return null;
        try {
            return IncidentState.valueOf(state.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Estado de incidencia inválido: " + state);
        }
    }

    private boolean hasAnyFilter(String tenantUserId, String state, String tipoId,
                                  LocalDateTime desde, LocalDateTime hasta, String search) {
        return notBlank(tenantUserId) || notBlank(state) || notBlank(tipoId)
                || desde != null || hasta != null || notBlank(search);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        var parts = sort.split(",");
        var field = mapSortField(parts[0].trim());
        var direction = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim())
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, field);
    }

    private String mapSortField(String field) {
        return switch (field) {
            case "created_at", "createdAt" -> "createdAt";
            case "updated_at", "updatedAt" -> "updatedAt";
            case "state" -> "state";
            default -> "createdAt";
        };
    }
}
