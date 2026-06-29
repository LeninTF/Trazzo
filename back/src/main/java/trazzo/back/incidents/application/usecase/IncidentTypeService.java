package trazzo.back.incidents.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.incidents.application.dto.command.CreateIncidentTypeCommand;
import trazzo.back.incidents.application.dto.command.PatchIncidentTypeCommand;
import trazzo.back.incidents.application.dto.result.IncidentTypeResult;
import trazzo.back.incidents.application.dto.result.PaginatedResult;
import trazzo.back.incidents.application.port.in.IncidentTypeUseCase;
import trazzo.back.incidents.application.port.out.IncidentTypeRepositoryPort;
import trazzo.back.incidents.domain.model.IncidentType;

import java.util.Optional;

@RequiredArgsConstructor
public class IncidentTypeService implements IncidentTypeUseCase {

    private final IncidentTypeRepositoryPort repository;

    @Override
    public IncidentTypeResult create(CreateIncidentTypeCommand command) {
        if (repository.existsByNombre(command.nombre())) {
            throw new IllegalArgumentException("El nombre del tipo de incidencia ya existe");
        }
        var type = IncidentType.create(command.nombre(), command.descripcion());
        var saved = repository.save(type);
        return toResult(saved);
    }

    @Override
    public Optional<IncidentTypeResult> findById(String id) {
        return repository.findById(id).map(this::toResult);
    }

    @Override
    public PaginatedResult<IncidentTypeResult> findAll(Boolean activo, int page, int size) {
        var types = repository.findAll(activo, page, size);
        var total = repository.count(activo);
        var results = types.stream().map(this::toResult).toList();
        var totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PaginatedResult<>(results, page, size, total, totalPages);
    }

    @Override
    public IncidentTypeResult patch(String id, PatchIncidentTypeCommand command) {
        var type = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tipo de incidencia no encontrado: " + id));

        if (command.nombre() != null) {
            type.rename(command.nombre());
        }
        if (command.descripcion() != null) {
            type.updateDescription(command.descripcion());
        }
        if (command.activo() != null) {
            if (command.activo()) {
                type.activate();
            } else {
                type.deactivate();
            }
        }

        var saved = repository.save(type);
        return toResult(saved);
    }

    private IncidentTypeResult toResult(IncidentType type) {
        return new IncidentTypeResult(
                type.getId(),
                type.getNombre(),
                type.getDescripcion(),
                type.isActivo(),
                type.getCreatedAt(),
                type.getUpdatedAt()
        );
    }
}
