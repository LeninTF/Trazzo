package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import trazzo.back.corehr.domain.model.AttendanceState;
import trazzo.back.corehr.domain.model.attendance.Attendance;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.AttendanceEntity;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper.AttendanceMapper;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.repository.AttendanceJpaRepository;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceRepositoryAdapterTest {

    @Mock
    private AttendanceJpaRepository attendanceRepo;

    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @InjectMocks
    private AttendanceRepositoryAdapter adapter;

    @Captor
    private ArgumentCaptor<MapSqlParameterSource> paramsCaptor;

    private final String id = "att-1";
    private final LocalDate date = LocalDate.of(2025, 1, 15);
    private final LocalDateTime now = LocalDateTime.now();

    private AttendanceEntity createEntity() {
        var e = new AttendanceEntity();
        e.setId(id);
        e.setTenantUserId(100L);
        e.setScheduleId(10L);
        e.setDeviceId(200L);
        e.setCheckIn(now);
        e.setCheckOut(now.plusHours(8));
        e.setAttendanceDate(date);
        e.setMinutesLate(0);
        e.setState(AttendanceState.PUNTUAL);
        return e;
    }

    private Attendance createDomain() {
        return Attendance.restore(id, 100L, 10L, 200L, now, now.plusHours(8), date, 0, AttendanceState.PUNTUAL, null, null, now, now);
    }

    @Test
    void save_shouldPersistAndReturnDomain() {
        var domain = createDomain();
        var entity = createEntity();
        when(attendanceRepo.saveAndFlush(any())).thenReturn(entity);

        var result = adapter.save(domain);

        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getTenantUserId()).isEqualTo(100L);
        verify(attendanceRepo).saveAndFlush(any());
    }

    @Test
    void findById_whenExists_shouldReturnDomain() {
        var entity = createEntity();
        when(attendanceRepo.findById(id)).thenReturn(Optional.of(entity));

        var result = adapter.findById(id);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(id);
    }

    @Test
    void findById_whenNotExists_shouldReturnEmpty() {
        when(attendanceRepo.findById("invalid")).thenReturn(Optional.empty());

        var result = adapter.findById("invalid");

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_withOrganizationFilter_shouldUseJdbcTemplate() {
        var domain = createDomain();
        when(namedParameterJdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(domain));

        var result = adapter.findAll("day", 1L, null, null, null, null, null, null, 0, 10, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(id);
        verify(namedParameterJdbcTemplate).query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class));
        verifyNoInteractions(attendanceRepo);
    }

    @Test
    void findAll_withOrganizationFilterByArea_shouldUseJdbcTemplate() {
        var domain = createDomain();
        when(namedParameterJdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(domain));

        var result = adapter.findAll("day", null, 1L, null, null, null, null, null, 0, 10, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void findAll_withOrganizationFilterByDepartamento_shouldUseJdbcTemplate() {
        var domain = createDomain();
        when(namedParameterJdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(domain));

        var result = adapter.findAll("day", null, null, 1L, null, null, null, null, 0, 10, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void findAll_withJpaFilters_shouldUseFindByFilters() {
        var entity = createEntity();
        when(attendanceRepo.findByFilters(100L, AttendanceState.PUNTUAL, date, date.plusDays(1), PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))))
                .thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findAll("day", null, null, null, date, date.plusDays(1), AttendanceState.PUNTUAL, 100L, 0, 10, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(id);
    }

    @Test
    void findAll_withoutAnyFilter_shouldUseFindAll() {
        var entity = createEntity();
        when(attendanceRepo.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))))
                .thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findAll("day", null, null, null, null, null, null, null, 0, 10, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void findAll_withOrganizationFilterAndSortByAttendanceDate_shouldUseNativeSort() {
        var domain = createDomain();
        when(namedParameterJdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(domain));

        var result = adapter.findAll("day", 1L, null, null, null, null, null, null, 0, 10, "attendance_date,asc");

        assertThat(result).hasSize(1);
        verify(namedParameterJdbcTemplate).query(contains("a.attendance_date"), any(MapSqlParameterSource.class), any(RowMapper.class));
    }

    @Test
    void findAll_withOrganizationFilterAndSortByCheckIn_shouldUseNativeSort() {
        var domain = createDomain();
        when(namedParameterJdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(domain));

        adapter.findAll("day", 1L, null, null, null, null, null, null, 0, 10, "checkIn,desc");

        verify(namedParameterJdbcTemplate).query(contains("a.check_in"), any(MapSqlParameterSource.class), any(RowMapper.class));
    }

    @Test
    void findAll_withOrganizationFilterAndSortByCheckOut_shouldUseNativeSort() {
        when(namedParameterJdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        adapter.findAll("day", null, 1L, null, null, null, null, null, 0, 10, "check_out,desc");

        verify(namedParameterJdbcTemplate).query(contains("a.check_out"), any(MapSqlParameterSource.class), any(RowMapper.class));
    }

    @Test
    void findAll_withOrganizationFilterAndSortByMinutesLate_shouldUseNativeSort() {
        when(namedParameterJdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        adapter.findAll("day", null, null, 1L, null, null, null, null, 0, 10, "minutes_late,asc");

        verify(namedParameterJdbcTemplate).query(contains("a.minutes_late"), any(MapSqlParameterSource.class), any(RowMapper.class));
    }

    @Test
    void findAll_withOrganizationFilterAndSortByState_shouldUseNativeSort() {
        when(namedParameterJdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        adapter.findAll("day", 1L, null, null, null, null, null, null, 0, 10, "state,desc");

        verify(namedParameterJdbcTemplate).query(contains("a.state"), any(MapSqlParameterSource.class), any(RowMapper.class));
    }

    @Test
    void findAll_withOrganizationFilterAndSortByUpdatedAt_shouldUseNativeSort() {
        when(namedParameterJdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        adapter.findAll("day", null, null, 1L, null, null, null, null, 0, 10, "updatedAt,desc");

        verify(namedParameterJdbcTemplate).query(contains("a.updated_at"), any(MapSqlParameterSource.class), any(RowMapper.class));
    }

    @Test
    void findAll_withOrganizationFilterAndUnknownSort_shouldDefaultToCreatedAt() {
        when(namedParameterJdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        adapter.findAll("day", 1L, null, null, null, null, null, null, 0, 10, "unknown_field,asc");

        verify(namedParameterJdbcTemplate).query(contains("a.created_at"), any(MapSqlParameterSource.class), any(RowMapper.class));
    }

    @Test
    void findAll_withOnlyTenantUserIdFilter_shouldUseFindByFilters() {
        var entity = createEntity();
        when(attendanceRepo.findByFilters(100L, null, null, null, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))))
                .thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findAll("day", null, null, null, null, null, null, 100L, 0, 10, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void findAll_withOnlyStateFilter_shouldUseFindByFilters() {
        var entity = createEntity();
        when(attendanceRepo.findByFilters(null, AttendanceState.TARDANZA, null, null, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))))
                .thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findAll("day", null, null, null, null, null, AttendanceState.TARDANZA, null, 0, 10, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void findAll_withOnlyDateFromFilter_shouldUseFindByFilters() {
        var entity = createEntity();
        when(attendanceRepo.findByFilters(null, null, date, null, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))))
                .thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findAll("day", null, null, null, date, null, null, null, 0, 10, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void findAll_withOnlyDateToFilter_shouldUseFindByFilters() {
        var entity = createEntity();
        when(attendanceRepo.findByFilters(null, null, null, date, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))))
                .thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findAll("day", null, null, null, null, date, null, null, 0, 10, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void findAll_withSortByAttendanceDateDESC_shouldApplyDirection() {
        var entity = createEntity();
        when(attendanceRepo.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        adapter.findAll("day", null, null, null, null, null, null, null, 0, 10, "attendanceDate,desc");

        var captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(attendanceRepo).findAll(captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("attendanceDate").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void findAll_withSortByCheckIn_shouldApplyField() {
        when(attendanceRepo.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        adapter.findAll("day", null, null, null, null, null, null, null, 0, 10, "checkIn");

        var captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(attendanceRepo).findAll(captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("checkIn")).isNotNull();
    }

    @Test
    void findAll_withSortByCheckOut_shouldApplyField() {
        when(attendanceRepo.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        adapter.findAll("day", null, null, null, null, null, null, null, 0, 10, "checkOut,asc");

        var captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(attendanceRepo).findAll(captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("checkOut")).isNotNull();
    }

    @Test
    void findAll_withSortByMinutesLate_shouldApplyField() {
        when(attendanceRepo.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        adapter.findAll("day", null, null, null, null, null, null, null, 0, 10, "minutesLate,desc");

        var captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(attendanceRepo).findAll(captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("minutesLate")).isNotNull();
    }

    @Test
    void findAll_withSortByState_shouldApplyField() {
        when(attendanceRepo.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        adapter.findAll("day", null, null, null, null, null, null, null, 0, 10, "state,asc");

        var captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(attendanceRepo).findAll(captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("state")).isNotNull();
    }

    @Test
    void findAll_withSortByCreatedAt_shouldApplyField() {
        when(attendanceRepo.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        adapter.findAll("day", null, null, null, null, null, null, null, 0, 10, "createdAt,desc");

        var captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(attendanceRepo).findAll(captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("createdAt")).isNotNull();
    }

    @Test
    void findAll_withSortByUpdatedAt_shouldApplyField() {
        when(attendanceRepo.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of()));

        adapter.findAll("day", null, null, null, null, null, null, null, 0, 10, "updatedAt");

        var captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(attendanceRepo).findAll(captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("updatedAt")).isNotNull();
    }

    @Test
    void count_withOrganizationFilter_shouldUseJdbcTemplate() {
        when(namedParameterJdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(5L);

        var result = adapter.count("day", 1L, null, null, null, null, null, null);

        assertThat(result).isEqualTo(5L);
    }

    @Test
    void count_withOrganizationFilterByArea_shouldUseJdbcTemplate() {
        when(namedParameterJdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(3L);

        var result = adapter.count("day", null, 1L, null, null, null, null, null);

        assertThat(result).isEqualTo(3L);
    }

    @Test
    void count_withOrganizationFilterByDepartamento_shouldUseJdbcTemplate() {
        when(namedParameterJdbcTemplate.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Long.class)))
                .thenReturn(7L);

        var result = adapter.count("day", null, null, 1L, null, null, null, null);

        assertThat(result).isEqualTo(7L);
    }

    @Test
    void count_withJpaFilters_shouldUseCountByFilters() {
        when(attendanceRepo.countByFilters(100L, AttendanceState.PUNTUAL, date, date.plusDays(1))).thenReturn(4L);

        var result = adapter.count("day", null, null, null, date, date.plusDays(1), AttendanceState.PUNTUAL, 100L);

        assertThat(result).isEqualTo(4L);
    }

    @Test
    void count_withoutAnyFilter_shouldUseTotalCount() {
        when(attendanceRepo.count()).thenReturn(10L);

        var result = adapter.count("day", null, null, null, null, null, null, null);

        assertThat(result).isEqualTo(10L);
    }

    @Test
    void count_withOnlyTenantUserId_shouldUseCountByFilters() {
        when(attendanceRepo.countByFilters(100L, null, null, null)).thenReturn(5L);

        var result = adapter.count("day", null, null, null, null, null, null, 100L);

        assertThat(result).isEqualTo(5L);
    }

    @Test
    void count_withOnlyState_shouldUseCountByFilters() {
        when(attendanceRepo.countByFilters(null, AttendanceState.TARDANZA, null, null)).thenReturn(3L);

        var result = adapter.count("day", null, null, null, null, null, AttendanceState.TARDANZA, null);

        assertThat(result).isEqualTo(3L);
    }

    @Test
    void count_withOnlyDateFrom_shouldUseCountByFilters() {
        when(attendanceRepo.countByFilters(null, null, date, null)).thenReturn(2L);

        var result = adapter.count("day", null, null, null, date, null, null, null);

        assertThat(result).isEqualTo(2L);
    }

    @Test
    void count_withOnlyDateTo_shouldUseCountByFilters() {
        when(attendanceRepo.countByFilters(null, null, null, date)).thenReturn(1L);

        var result = adapter.count("day", null, null, null, null, date, null, null);

        assertThat(result).isEqualTo(1L);
    }

    @Test
    void findByTenantUserIdAndDate_whenExists_shouldReturnDomain() {
        var entity = createEntity();
        when(attendanceRepo.findByTenantUserIdAndAttendanceDate(100L, date)).thenReturn(Optional.of(entity));

        var result = adapter.findByTenantUserIdAndDate(100L, date);

        assertThat(result).isPresent();
        assertThat(result.get().getTenantUserId()).isEqualTo(100L);
    }

    @Test
    void findByTenantUserIdAndDate_whenNotExists_shouldReturnEmpty() {
        when(attendanceRepo.findByTenantUserIdAndAttendanceDate(99L, date)).thenReturn(Optional.empty());

        var result = adapter.findByTenantUserIdAndDate(99L, date);

        assertThat(result).isEmpty();
    }

    @Test
    void existsByOfflineEventIdAndDeviceCode_whenExists_shouldReturnTrue() {
        when(attendanceRepo.existsByOfflineEventIdAndDeviceCode(1, "DVC-001")).thenReturn(true);

        var result = adapter.existsByOfflineEventIdAndDeviceCode(1, "DVC-001");

        assertThat(result).isTrue();
    }

    @Test
    void existsByOfflineEventIdAndDeviceCode_whenNotExists_shouldReturnFalse() {
        when(attendanceRepo.existsByOfflineEventIdAndDeviceCode(99, "DVC-999")).thenReturn(false);

        var result = adapter.existsByOfflineEventIdAndDeviceCode(99, "DVC-999");

        assertThat(result).isFalse();
    }

    @SuppressWarnings("unchecked")
    private RowMapper<Attendance> getRowMapper() throws Exception {
        var field = AttendanceRepositoryAdapter.class.getDeclaredField("ATTENDANCE_ROW_MAPPER");
        field.setAccessible(true);
        return (RowMapper<Attendance>) field.get(null);
    }

    @Test
    void rowMapper_shouldMapAllFields_whenAllPresent() throws Exception {
        var rs = mock(ResultSet.class);
        when(rs.getString("id")).thenReturn(id);
        when(rs.getLong("tenant_user_id")).thenReturn(100L);
        when(rs.getLong("schedule_id")).thenReturn(10L);
        when(rs.wasNull()).thenReturn(false);
        when(rs.getLong("device_id")).thenReturn(200L);
        when(rs.getTimestamp("check_in")).thenReturn(Timestamp.valueOf(now));
        when(rs.getTimestamp("check_out")).thenReturn(Timestamp.valueOf(now.plusHours(8)));
        when(rs.getDate("attendance_date")).thenReturn(java.sql.Date.valueOf(date));
        when(rs.getInt("minutes_late")).thenReturn(0);
        when(rs.getString("state")).thenReturn("PUNTUAL");
        when(rs.getInt("offline_event_id")).thenReturn(0);
        when(rs.getString("device_code")).thenReturn("DVC-001");
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(now));
        when(rs.getTimestamp("updated_at")).thenReturn(Timestamp.valueOf(now));

        var mapper = getRowMapper();
        var attendance = mapper.mapRow(rs, 0);

        assertThat(attendance.getId()).isEqualTo(id);
        assertThat(attendance.getTenantUserId()).isEqualTo(100L);
        assertThat(attendance.getScheduleId()).isEqualTo(10L);
        assertThat(attendance.getDeviceId()).isEqualTo(200L);
        assertThat(attendance.getCheckIn()).isEqualTo(now);
        assertThat(attendance.getCheckOut()).isEqualTo(now.plusHours(8));
    }

    @Test
    void rowMapper_shouldHandleNullScheduleIdAndDeviceId() throws Exception {
        var rs = mock(ResultSet.class);
        when(rs.getString("id")).thenReturn(id);
        when(rs.getLong("tenant_user_id")).thenReturn(100L);
        when(rs.getLong("schedule_id")).thenReturn(0L);
        when(rs.wasNull()).thenReturn(true);
        when(rs.getLong("device_id")).thenReturn(0L);
        when(rs.getTimestamp("check_in")).thenReturn(null);
        when(rs.getTimestamp("check_out")).thenReturn(null);
        when(rs.getDate("attendance_date")).thenReturn(java.sql.Date.valueOf(date));
        when(rs.getInt("minutes_late")).thenReturn(5);
        when(rs.getString("state")).thenReturn("TARDANZA");
        when(rs.getInt("offline_event_id")).thenReturn(0);
        when(rs.getString("device_code")).thenReturn(null);
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(now));
        when(rs.getTimestamp("updated_at")).thenReturn(Timestamp.valueOf(now));

        var mapper = getRowMapper();
        var attendance = mapper.mapRow(rs, 0);

        assertThat(attendance.getScheduleId()).isNull();
        assertThat(attendance.getDeviceId()).isNull();
        assertThat(attendance.getCheckIn()).isNull();
        assertThat(attendance.getCheckOut()).isNull();
        assertThat(attendance.getMinutesLate()).isEqualTo(5);
    }

    @Test
    void rowMapper_shouldHandleNullOfflineEventId() throws Exception {
        var rs = mock(ResultSet.class);
        when(rs.getString("id")).thenReturn(id);
        when(rs.getLong("tenant_user_id")).thenReturn(100L);
        when(rs.getLong("schedule_id")).thenReturn(10L);
        when(rs.wasNull()).thenReturn(false);
        when(rs.getLong("device_id")).thenReturn(200L);
        when(rs.getTimestamp("check_in")).thenReturn(Timestamp.valueOf(now));
        when(rs.getTimestamp("check_out")).thenReturn(Timestamp.valueOf(now.plusHours(8)));
        when(rs.getDate("attendance_date")).thenReturn(java.sql.Date.valueOf(date));
        when(rs.getInt("minutes_late")).thenReturn(0);
        when(rs.getString("state")).thenReturn("PUNTUAL");
        when(rs.getInt("offline_event_id")).thenReturn(0);
        when(rs.wasNull()).thenReturn(true);
        when(rs.getString("device_code")).thenReturn("DVC-001");
        when(rs.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(now));
        when(rs.getTimestamp("updated_at")).thenReturn(Timestamp.valueOf(now));

        var mapper = getRowMapper();
        var attendance = mapper.mapRow(rs, 0);

        assertThat(attendance.getOfflineEventId()).isNull();
    }

    @Test
    void buildParams_shouldSetOffsetCorrectlyForNegativePage() {
        when(namedParameterJdbcTemplate.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());

        adapter.findAll("day", 1L, null, null, null, null, null, null, -1, 10, null);

        verify(namedParameterJdbcTemplate).query(anyString(), paramsCaptor.capture(), any(RowMapper.class));
        var params = paramsCaptor.getValue();
        assertThat(params.getValue("offset")).isEqualTo(0);
        assertThat(params.getValue("limit")).isEqualTo(10);
    }
}
