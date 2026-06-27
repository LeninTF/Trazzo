package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.corehr.application.port.out.ShiftRepositoryPort;
import trazzo.back.corehr.domain.model.schedule.Shift;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper.ShiftMapper;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.repository.ShiftJpaRepository;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShiftRepositoryAdapter implements ShiftRepositoryPort {

    private final ShiftJpaRepository shiftRepo;

    @Override
    @Transactional
    public Shift save(Shift shift) {
        var entity = ShiftMapper.toEntity(shift);
        var saved = shiftRepo.save(entity);
        return ShiftMapper.toDomain(saved);
    }

    @Override
    public Optional<Shift> findById(Long id) {
        return shiftRepo.findById(id).map(ShiftMapper::toDomain);
    }

    @Override
    public List<Shift> findAll(String search, int page, int size, String sort) {
        var sortObj = parseSort(sort);
        var pageable = PageRequest.of(page, size, sortObj);
        return (search == null || search.isBlank()
                ? shiftRepo.findAll(pageable)
                : shiftRepo.findByNameContainingIgnoreCase(search, pageable))
                .stream()
                .map(ShiftMapper::toDomain)
                .toList();
    }

    @Override
    public long count(String search) {
        if (search == null || search.isBlank()) {
            return shiftRepo.count();
        }
        return shiftRepo.countByNameContainingIgnoreCase(search);
    }

    @Override
    public boolean existsByName(String name) {
        return shiftRepo.existsByName(name);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        shiftRepo.deleteById(id);
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
            case "name" -> "name";
            case "created_at", "createdAt" -> "createdAt";
            case "updated_at", "updatedAt" -> "updatedAt";
            default -> "createdAt";
        };
    }
}
