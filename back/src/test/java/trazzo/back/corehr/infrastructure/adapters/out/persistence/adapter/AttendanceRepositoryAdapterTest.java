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
}
