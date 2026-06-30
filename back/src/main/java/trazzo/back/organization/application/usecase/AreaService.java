package trazzo.back.organization.application.usecase;

import lombok.RequiredArgsConstructor;
import trazzo.back.organization.application.dto.command.CreateAreaCommand;
import trazzo.back.organization.application.dto.command.UpdateAreaCommand;
import trazzo.back.organization.application.dto.result.AreaResult;
import trazzo.back.organization.application.dto.result.PaginatedResult;
import trazzo.back.organization.application.port.in.AreaUseCase;
import trazzo.back.organization.application.port.out.AreaRepositoryPort;
import trazzo.back.organization.application.port.out.BranchRepositoryPort;
import trazzo.back.organization.domain.exception.DuplicateOrgNameException;
import trazzo.back.organization.domain.exception.OrgNotFoundException;
import trazzo.back.organization.domain.model.business.Area;

import java.util.Optional;

@RequiredArgsConstructor
public class AreaService implements AreaUseCase {

    private final AreaRepositoryPort areaRepository;
    private final BranchRepositoryPort branchRepository;

    @Override
    public AreaResult create(CreateAreaCommand command) {
        branchRepository.findById(command.branchId())
                .orElseThrow(() -> new OrgNotFoundException("Branch not found: " + command.branchId()));
        if (areaRepository.existsByBranchIdAndName(command.branchId(), command.name())) {
            throw new DuplicateOrgNameException("Area '" + command.name() + "' already exists in this branch");
        }
        var area = Area.create(command.branchId(), command.name(), command.description());
        return toResult(areaRepository.save(area));
    }

    @Override
    public Optional<AreaResult> findById(Long id) {
        return areaRepository.findById(id).map(this::toResult);
    }

    @Override
    public PaginatedResult<AreaResult> findAll(Long branchId, Boolean state, String search, int page, int size, String sort) {
        var items = areaRepository.findAll(branchId, state, search, page, size, sort);
        var total = areaRepository.count(branchId, state, search);
        var totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        return new PaginatedResult<>(items.stream().map(this::toResult).toList(), page, size, total, totalPages);
    }

    @Override
    public AreaResult update(Long id, UpdateAreaCommand command) {
        var area = areaRepository.findById(id)
                .orElseThrow(() -> new OrgNotFoundException("Area not found: " + id));
        if (areaRepository.existsByBranchIdAndNameAndIdNot(area.getBranchId(), command.name(), id)) {
            throw new DuplicateOrgNameException("Area '" + command.name() + "' already exists in this branch");
        }
        area.update(command.name(), command.description());
        return toResult(areaRepository.save(area));
    }

    @Override
    public void delete(Long id) {
        var area = areaRepository.findById(id)
                .orElseThrow(() -> new OrgNotFoundException("Area not found: " + id));
        area.softDelete();
        areaRepository.save(area);
    }

    private AreaResult toResult(Area area) {
        return new AreaResult(
                area.getId(), area.getBranchId(), area.getName(), area.getDescription(),
                area.isState(), area.getCreatedAt(), area.getUpdatedAt(), area.getDeletedAt()
        );
    }
}
