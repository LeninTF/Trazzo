package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import trazzo.back.corehr.domain.model.schedule.Schedule;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.ScheduleEntity;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper.ScheduleMapper;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.repository.ScheduleJpaRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleRepositoryAdapterTest {

    @Mock
    private ScheduleJpaRepository scheduleRepo;

    @InjectMocks
    private ScheduleRepositoryAdapter adapter;

    @Test
    void shouldSave() {
        var now = LocalDateTime.now();
        var domain = Schedule.restore(1L, 10L, "M", "d",
                LocalTime.of(8, 0), LocalTime.of(17, 0), now, now);

        var entity = ScheduleMapper.toEntity(domain);
        when(scheduleRepo.save(any())).thenReturn(entity);

        var result = adapter.save(domain);

        assertEquals(domain.getId(), result.getId());
        verify(scheduleRepo).save(any());
    }

    @Test
    void shouldFindById() {
        var now = LocalDateTime.now();
        var entity = new ScheduleEntity();
        entity.setId(1L);
        entity.setShiftId(10L);
        entity.setName("M");
        entity.setEntryTime(LocalTime.of(8, 0));
        entity.setDepartureTime(LocalTime.of(17, 0));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        when(scheduleRepo.findById(1L)).thenReturn(Optional.of(entity));

        var result = adapter.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    void shouldReturnEmptyWhenNotFound() {
        when(scheduleRepo.findById(99L)).thenReturn(Optional.empty());

        assertTrue(adapter.findById(99L).isEmpty());
    }

    @Test
    void shouldFindAll() {
        var now = LocalDateTime.now();
        var entity = new ScheduleEntity();
        entity.setId(1L);
        entity.setShiftId(10L);
        entity.setName("M");
        entity.setEntryTime(LocalTime.of(8, 0));
        entity.setDepartureTime(LocalTime.of(17, 0));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(scheduleRepo.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findAll(null, 0, 10, null);

        assertEquals(1, result.size());
    }

    @Test
    void shouldFindAllByShiftId() {
        var now = LocalDateTime.now();
        var entity = new ScheduleEntity();
        entity.setId(1L);
        entity.setShiftId(10L);
        entity.setName("M");
        entity.setEntryTime(LocalTime.of(8, 0));
        entity.setDepartureTime(LocalTime.of(17, 0));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(scheduleRepo.findByShiftId(10L, pageable)).thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findAll(10L, 0, 10, null);

        assertEquals(1, result.size());
    }

    @Test
    void shouldCount() {
        when(scheduleRepo.count()).thenReturn(5L);

        assertEquals(5L, adapter.count(null));
    }

    @Test
    void shouldCountByShiftId() {
        when(scheduleRepo.countByShiftId(10L)).thenReturn(3L);

        assertEquals(3L, adapter.count(10L));
    }

    @Test
    void shouldCheckExistsByShiftId() {
        when(scheduleRepo.existsByShiftId(10L)).thenReturn(true);

        assertTrue(adapter.existsByShiftId(10L));
    }

    @Test
    void shouldDeleteById() {
        adapter.deleteById(1L);

        verify(scheduleRepo).deleteById(1L);
    }
}
