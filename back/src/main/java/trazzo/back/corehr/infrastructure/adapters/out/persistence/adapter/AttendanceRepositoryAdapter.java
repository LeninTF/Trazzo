package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import trazzo.back.corehr.application.port.out.AttendanceRepositoryPort;
import trazzo.back.corehr.domain.model.AttendanceState;
import trazzo.back.corehr.domain.model.attendance.Attendance;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.AttendanceEntity;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper.AttendanceMapper;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.repository.AttendanceJpaRepository;
import trazzo.back.shared.util.SortUtils;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttendanceRepositoryAdapter implements AttendanceRepositoryPort {

    private final AttendanceJpaRepository attendanceRepo;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private static final RowMapper<Attendance> ATTENDANCE_ROW_MAPPER = new RowMapper<>() {
        @Override
        public Attendance mapRow(ResultSet rs, int rowNum) throws SQLException {
            return Attendance.restore(
                    rs.getString("id"),
                    rs.getLong("tenant_user_id"),
                    getNullableLong(rs, "schedule_id"),
                    getNullableLong(rs, "device_id"),
                    rs.getTimestamp("check_in") != null ? rs.getTimestamp("check_in").toLocalDateTime() : null,
                    rs.getTimestamp("check_out") != null ? rs.getTimestamp("check_out").toLocalDateTime() : null,
                    rs.getDate("attendance_date").toLocalDate(),
                    rs.getInt("minutes_late"),
                    AttendanceState.valueOf(rs.getString("state")),
                    getNullableInt(rs, "offline_event_id"),
                    rs.getString("device_code"),
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    rs.getTimestamp("updated_at").toLocalDateTime()
            );
        }
    };

    @Override
    @Transactional
    public Attendance save(Attendance attendance) {
        var entity = AttendanceMapper.toEntity(attendance);
        var saved = attendanceRepo.saveAndFlush(entity);
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
        if (hasOrganizationFilter(branchId, areaId, departamentoId)) {
            var params = buildParams(branchId, areaId, departamentoId, dateFrom, dateTo, state, tenantUserId, page, size);
            return namedParameterJdbcTemplate.query(buildSelectSql(sort), params, ATTENDANCE_ROW_MAPPER);
        }
        var sortObj = SortUtils.parseSort(sort, f -> switch (f) {
            case "attendance_date", "attendanceDate" -> "attendanceDate";
            case "check_in", "checkIn" -> "checkIn";
            case "check_out", "checkOut" -> "checkOut";
            case "minutes_late", "minutesLate" -> "minutesLate";
            case "state" -> "state";
            case "created_at", "createdAt" -> "createdAt";
            case "updated_at", "updatedAt" -> "updatedAt";
            default -> "createdAt";
        });
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
        if (hasOrganizationFilter(branchId, areaId, departamentoId)) {
            var params = buildParams(branchId, areaId, departamentoId, dateFrom, dateTo, state, tenantUserId, null, null);
            return namedParameterJdbcTemplate.queryForObject(buildCountSql(), params, Long.class);
        }
        if (hasAnyFilter(tenantUserId, state, dateFrom, dateTo)) {
            return attendanceRepo.countByFilters(tenantUserId, state, dateFrom, dateTo);
        }
        return attendanceRepo.count();
    }

    @Override
    public Optional<Attendance> findByTenantUserIdAndDate(Long tenantUserId, LocalDate date) {
        return attendanceRepo.findByTenantUserIdAndAttendanceDate(tenantUserId, date)
                .map(AttendanceMapper::toDomain);
    }

    @Override
    public boolean existsByOfflineEventIdAndDeviceCode(Integer offlineEventId, String deviceCode) {
        return attendanceRepo.existsByOfflineEventIdAndDeviceCode(offlineEventId, deviceCode);
    }

    private boolean hasOrganizationFilter(Long branchId, Long areaId, Long departamentoId) {
        return branchId != null || areaId != null || departamentoId != null;
    }

    private boolean hasAnyFilter(Long tenantUserId, AttendanceState state,
                                  LocalDate dateFrom, LocalDate dateTo) {
        return tenantUserId != null || state != null || dateFrom != null || dateTo != null;
    }

    private MapSqlParameterSource buildParams(Long branchId, Long areaId, Long departamentoId,
                                              LocalDate dateFrom, LocalDate dateTo, AttendanceState state,
                                              Long tenantUserId, Integer page, Integer size) {
        var params = new MapSqlParameterSource()
                .addValue("branchId", branchId)
                .addValue("areaId", areaId)
                .addValue("departamentoId", departamentoId)
                .addValue("dateFrom", dateFrom != null ? Date.valueOf(dateFrom) : null)
                .addValue("dateTo", dateTo != null ? Date.valueOf(dateTo) : null)
                .addValue("state", state != null ? state.name() : null)
                .addValue("tenantUserId", tenantUserId)
                .addValue("currentDate", Date.valueOf(LocalDate.now()));
        if (page != null && size != null) {
            params.addValue("limit", size);
            params.addValue("offset", Math.max(page, 0) * size);
        }
        return params;
    }

    private String buildSelectSql(String sort) {
        var nativeSort = SortUtils.parseNativeSort(sort);
        return """
                SELECT DISTINCT a.*
                FROM attendances a
                LEFT JOIN tenant_user_department tud ON tud.tenant_user_id = a.tenant_user_id
                LEFT JOIN department d ON d.id = tud.department_id
                LEFT JOIN area ar ON ar.id = d.area_id
                WHERE (:tenantUserId IS NULL OR a.tenant_user_id = :tenantUserId)
                  AND (:state IS NULL OR a.state = :state)
                  AND (:dateFrom IS NULL OR a.attendance_date >= :dateFrom)
                  AND (:dateTo IS NULL OR a.attendance_date <= :dateTo)
                  AND (:departamentoId IS NULL OR d.id = :departamentoId)
                  AND (:areaId IS NULL OR ar.id = :areaId)
                  AND (:branchId IS NULL OR ar.branch_id = :branchId)
                  AND (tud.id IS NULL OR tud.end_date IS NULL OR tud.end_date >= :currentDate)
                ORDER BY %s %s
                LIMIT :limit OFFSET :offset
                """.formatted(nativeSort.field(), nativeSort.direction());
    }

    private String buildCountSql() {
        return """
                SELECT COUNT(DISTINCT a.id)
                FROM attendances a
                LEFT JOIN tenant_user_department tud ON tud.tenant_user_id = a.tenant_user_id
                LEFT JOIN department d ON d.id = tud.department_id
                LEFT JOIN area ar ON ar.id = d.area_id
                WHERE (:tenantUserId IS NULL OR a.tenant_user_id = :tenantUserId)
                  AND (:state IS NULL OR a.state = :state)
                  AND (:dateFrom IS NULL OR a.attendance_date >= :dateFrom)
                  AND (:dateTo IS NULL OR a.attendance_date <= :dateTo)
                  AND (:departamentoId IS NULL OR d.id = :departamentoId)
                  AND (:areaId IS NULL OR ar.id = :areaId)
                  AND (:branchId IS NULL OR ar.branch_id = :branchId)
                  AND (tud.id IS NULL OR tud.end_date IS NULL OR tud.end_date >= :currentDate)
                """;
    }

    private static Long getNullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static Integer getNullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }
}
