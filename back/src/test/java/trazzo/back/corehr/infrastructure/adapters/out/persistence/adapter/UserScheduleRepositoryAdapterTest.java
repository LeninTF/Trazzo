package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import trazzo.back.corehr.domain.model.schedule.UserSchedule;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.UserScheduleEntity;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.repository.UserScheduleJpaRepository;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserScheduleRepositoryAdapterTest {

    @Mock
    private UserScheduleJpaRepository userScheduleRepo;

    @InjectMocks
    private UserScheduleRepositoryAdapter adapter;

    private final LocalTime entryTime = LocalTime.of(8, 0);
    private final LocalTime departureTime = LocalTime.of(17, 0);
    private final LocalDateTime now = LocalDateTime.now();

    private UserScheduleEntity createEntity(Long id) {
        var e = new UserScheduleEntity();
        e.setId(id);
        e.setTenantUserId(100L);
        e.setScheduleId(10L);
        e.setDescription("User schedule");
        e.setEntryTime(entryTime);
        e.setDepartureTime(departureTime);
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        return e;
    }

    @Test
    void save_shouldPersistAndReturnDomain() {
        var domain = UserSchedule.restore(null, 100L, 10L, "desc", entryTime, departureTime, now, now);
        var entity = createEntity(1L);
        when(userScheduleRepo.save(any())).thenReturn(entity);

        var result = adapter.save(domain);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTenantUserId()).isEqualTo(100L);
        verify(userScheduleRepo).save(any());
    }

    @Test
    void findById_whenExists_shouldReturnDomain() {
        var entity = createEntity(1L);
        when(userScheduleRepo.findById(1L)).thenReturn(Optional.of(entity));

        var result = adapter.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void findById_whenNotExists_shouldReturnEmpty() {
        when(userScheduleRepo.findById(99L)).thenReturn(Optional.empty());

        var result = adapter.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_shouldReturnMappedList() {
        var entity = createEntity(1L);
        when(userScheduleRepo.findByTenantUserIdAndScheduleId(100L, 10L, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findAll(100L, 10L, 0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScheduleId()).isEqualTo(10L);
    }

    @Test
    void findAll_withNullFilters_shouldUseNullParams() {
        var entity = createEntity(1L);
        when(userScheduleRepo.findByTenantUserIdAndScheduleId(null, null, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findAll(null, null, 0, 10);

        assertThat(result).hasSize(1);
    }

    @Test
    void count_shouldReturnCount() {
        when(userScheduleRepo.countByTenantUserIdAndScheduleId(100L, 10L)).thenReturn(3L);

        var result = adapter.count(100L, 10L);

        assertThat(result).isEqualTo(3L);
    }

    @Test
    void deleteById_shouldDelegate() {
        adapter.deleteById(1L);

        verify(userScheduleRepo).deleteById(1L);
    }

    @Test
    void existsByTenantUserId_shouldReturnTrueWhenExists() {
        when(userScheduleRepo.existsByTenantUserId(100L)).thenReturn(true);

        var result = adapter.existsByTenantUserId(100L);

        assertThat(result).isTrue();
    }

    @Test
    void existsByTenantUserId_shouldReturnFalseWhenNotExists() {
        when(userScheduleRepo.existsByTenantUserId(99L)).thenReturn(false);

        var result = adapter.existsByTenantUserId(99L);

        assertThat(result).isFalse();
    }

    @Test
    void findByTenantUserId_shouldReturnMappedList() {
        var entity = createEntity(1L);
        when(userScheduleRepo.findByTenantUserId(100L)).thenReturn(List.of(entity));

        var result = adapter.findByTenantUserId(100L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTenantUserId()).isEqualTo(100L);
    }

    @Test
    void findByTenantUserId_whenEmpty_shouldReturnEmptyList() {
        when(userScheduleRepo.findByTenantUserId(99L)).thenReturn(List.of());

        var result = adapter.findByTenantUserId(99L);

        assertThat(result).isEmpty();
    }
}
