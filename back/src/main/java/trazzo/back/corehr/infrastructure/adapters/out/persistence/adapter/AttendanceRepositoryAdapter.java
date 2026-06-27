package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.corehr.application.port.out.AttendanceRepositoryPort;
import trazzo.back.corehr.domain.model.AttendanceState;
import trazzo.back.corehr.domain.model.attendance.Attendance;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.AttendanceEntity;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper.AttendanceMapper;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.repository.AttendanceJpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceRepositoryAdapter implements AttendanceRepositoryPort {

    private final AttendanceJpaRepository attendanceRepo;

    @Override
    @Transactional
    public Attendance save(Attendance attendance) {
        var entity = AttendanceMapper.toEntity(attendance);
        var saved = attendanceRepo.save(entity);
        return AttendanceMapper.toDomain(saved);
    }

    @Override
    public Optional<Attendance> findById(String id) {
        return attendanceRepo.findById(id).map(AttendanceMapper::toDomain);
    }

    @Override
    public List<Attendance> findAll(String scope, Long branchId, Long areaId, Long departamentoId,
                                     LocalDate dateFrom, LocalDate dateTo, AttendanceState state,
                                     Long tenantUserId, int page, int size, String sort) {
        var sortObj = parseSort(sort);
        var pageable = PageRequest.of(page, size, sortObj);
        Page<AttendanceEntity> result;

        if (hasAnyFilter(tenantUserId, state, dateFrom, dateTo)) {
            result = attendanceRepo.findByFilters(tenantUserId, state, dateFrom, dateTo, pageable);
        } else {
            result = attendanceRepo.findAll(pageable);
        }

        return result.stream()
                .map(AttendanceMapper::toDomain)
                .toList();
    }

    @Override
    public long count(String scope, Long branchId, Long areaId, Long departamentoId,
                       LocalDate dateFrom, LocalDate dateTo, AttendanceState state,
                       Long tenantUserId) {
        if (hasAnyFilter(tenantUserId, state, dateFrom, dateTo)) {
            return attendanceRepo.countByFilters(tenantUserId, state, dateFrom, dateTo);
        }
        return attendanceRepo.count();
    }

    private boolean hasAnyFilter(Long tenantUserId, AttendanceState state,
                                  LocalDate dateFrom, LocalDate dateTo) {
        return tenantUserId != null || state != null || dateFrom != null || dateTo != null;
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
            case "attendance_date", "attendanceDate" -> "attendanceDate";
            case "check_in", "checkIn" -> "checkIn";
            case "check_out", "checkOut" -> "checkOut";
            case "minutes_late", "minutesLate" -> "minutesLate";
            case "state" -> "state";
            case "created_at", "createdAt" -> "createdAt";
            case "updated_at", "updatedAt" -> "updatedAt";
            default -> "createdAt";
        };
    }
}
