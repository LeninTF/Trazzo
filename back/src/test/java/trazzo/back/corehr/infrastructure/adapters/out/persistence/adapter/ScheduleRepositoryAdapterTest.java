package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import trazzo.back.corehr.domain.model.schedule.Schedule;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.ScheduleEntity;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.repository.ScheduleJpaRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleRepositoryAdapterTest {

    @Mock
    private ScheduleJpaRepository scheduleRepo;

    @InjectMocks
    private ScheduleRepositoryAdapter adapter;

    private final LocalTime entryTime = LocalTime.of(8, 0);
    private final LocalTime departureTime = LocalTime.of(17, 0);
    private final LocalDateTime now = LocalDateTime.now();

    private ScheduleEntity createEntity(Long id) {
        var e = new ScheduleEntity();
        e.setId(id);
        e.setShiftId(10L);
        e.setName("Morning Shift");
        e.setDescription("Regular morning schedule");
        e.setEntryTime(entryTime);
        e.setDepartureTime(departureTime);
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        return e;
    }

    @Test
    void save_shouldPersistAndReturnDomain() {
        var domain = Schedule.restore(null, 10L, "Morning", "desc", entryTime, departureTime, now, now);
        var entity = createEntity(1L);
        when(scheduleRepo.save(any())).thenReturn(entity);

        var result = adapter.save(domain);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Morning Shift");
        verify(scheduleRepo).save(any());
    }

    @Test
    void findById_whenExists_shouldReturnDomain() {
        var entity = createEntity(1L);
        when(scheduleRepo.findById(1L)).thenReturn(Optional.of(entity));

        var result = adapter.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void findById_whenNotExists_shouldReturnEmpty() {
        when(scheduleRepo.findById(99L)).thenReturn(Optional.empty());

        var result = adapter.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_withShiftId_shouldFilterByShift() {
        var entity = createEntity(1L);
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(scheduleRepo.findByShiftId(10L, pageable)).thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findAll(10L, 0, 10, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getShiftId()).isEqualTo(10L);
    }

    @Test
    void findAll_withoutShiftId_shouldReturnAll() {
        var entity = createEntity(1L);
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(scheduleRepo.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findAll(null, 0, 10, null);

        assertThat(result).hasSize(1);
    }

    @Test
    void findAll_withSortByName_shouldApplySort() {
        var entity = createEntity(1L);
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name"));
        when(scheduleRepo.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findAll(null, 0, 10, "name");

        assertThat(result).hasSize(1);
    }

    @Test
    void findAll_withSortByEntryTime_shouldApplySort() {
        var entity = createEntity(1L);
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "entryTime"));
        when(scheduleRepo.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findAll(null, 0, 10, "entryTime");

        assertThat(result).hasSize(1);
    }

    @Test
    void findAll_withSortByEntryTimeSnake_shouldApplySort() {
        when(scheduleRepo.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        adapter.findAll(null, 0, 10, "entry_time,desc");

        var captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(scheduleRepo).findAll(captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("entryTime").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void findAll_withSortByDepartureTime_shouldApplySort() {
        when(scheduleRepo.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        adapter.findAll(null, 0, 10, "departureTime");

        var captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(scheduleRepo).findAll(captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("departureTime")).isNotNull();
    }

    @Test
    void findAll_withSortByDepartureTimeSnake_shouldApplySort() {
        when(scheduleRepo.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        adapter.findAll(null, 0, 10, "departure_time,asc");

        var captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(scheduleRepo).findAll(captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("departureTime")).isNotNull();
    }

    @Test
    void findAll_withSortByCreatedAt_shouldApplySort() {
        when(scheduleRepo.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        adapter.findAll(null, 0, 10, "createdAt,desc");

        var captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(scheduleRepo).findAll(captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void findAll_withSortByCreatedAtSnake_shouldApplySort() {
        when(scheduleRepo.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        adapter.findAll(null, 0, 10, "created_at,asc");

        var captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(scheduleRepo).findAll(captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void findAll_withSortByUpdatedAt_shouldApplySort() {
        when(scheduleRepo.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        adapter.findAll(null, 0, 10, "updatedAt");

        var captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(scheduleRepo).findAll(captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("updatedAt")).isNotNull();
    }

    @Test
    void findAll_withSortByUpdatedAtSnake_shouldApplySort() {
        when(scheduleRepo.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        adapter.findAll(null, 0, 10, "updated_at,desc");

        var captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(scheduleRepo).findAll(captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("updatedAt")).isNotNull();
    }

    @Test
    void findAll_withUnknownSortField_shouldDefaultToCreatedAt() {
        when(scheduleRepo.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        adapter.findAll(null, 0, 10, "unknownField,asc");

        var captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(scheduleRepo).findAll(captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("createdAt")).isNotNull();
    }

    @Test
    void count_withShiftId_shouldReturnCountByShift() {
        when(scheduleRepo.countByShiftId(10L)).thenReturn(3L);

        var result = adapter.count(10L);

        assertThat(result).isEqualTo(3L);
    }

    @Test
    void count_withoutShiftId_shouldReturnTotalCount() {
        when(scheduleRepo.count()).thenReturn(10L);

        var result = adapter.count(null);

        assertThat(result).isEqualTo(10L);
    }

    @Test
    void existsByShiftId_shouldReturnTrueWhenExists() {
        when(scheduleRepo.existsByShiftId(10L)).thenReturn(true);

        var result = adapter.existsByShiftId(10L);

        assertThat(result).isTrue();
    }

    @Test
    void existsByShiftId_shouldReturnFalseWhenNotExists() {
        when(scheduleRepo.existsByShiftId(99L)).thenReturn(false);

        var result = adapter.existsByShiftId(99L);

        assertThat(result).isFalse();
    }

    @Test
    void deleteById_shouldDelegate() {
        adapter.deleteById(1L);

        verify(scheduleRepo).deleteById(1L);
    }

    @Test
    void countActiveSchedulesByShiftId_shouldDelegate() {
        when(scheduleRepo.countByShiftId(10L)).thenReturn(2L);

        var result = adapter.countActiveSchedulesByShiftId(10L);

        assertThat(result).isEqualTo(2L);
    }
}
