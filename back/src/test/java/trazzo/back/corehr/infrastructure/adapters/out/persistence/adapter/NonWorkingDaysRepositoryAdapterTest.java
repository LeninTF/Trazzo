package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import trazzo.back.corehr.domain.model.schedule.NonWorkingDays;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.NonWorkingDaysEntity;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.repository.NonWorkingDaysJpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NonWorkingDaysRepositoryAdapterTest {

    @Mock
    private NonWorkingDaysJpaRepository nonWorkingDaysRepo;

    @InjectMocks
    private NonWorkingDaysRepositoryAdapter adapter;

    private final LocalDate date = LocalDate.of(2025, 1, 1);
    private final LocalDateTime now = LocalDateTime.now();

    private NonWorkingDaysEntity createEntity(Long id) {
        var e = new NonWorkingDaysEntity();
        e.setId(id);
        e.setDate(date);
        e.setDescription("New Year");
        e.setRecurring(true);
        e.setCreatedAt(now);
        return e;
    }

    @Test
    void save_shouldPersistAndReturnDomain() {
        var domain = NonWorkingDays.restore(null, date, "New Year", true, now);
        var entity = createEntity(1L);
        when(nonWorkingDaysRepo.save(any())).thenReturn(entity);

        var result = adapter.save(domain);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getDate()).isEqualTo(date);
        assertThat(result.getDescription()).isEqualTo("New Year");
        assertThat(result.isRecurring()).isTrue();
        verify(nonWorkingDaysRepo).save(any());
    }

    @Test
    void findById_whenExists_shouldReturnDomain() {
        var entity = createEntity(1L);
        when(nonWorkingDaysRepo.findById(1L)).thenReturn(Optional.of(entity));

        var result = adapter.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void findById_whenNotExists_shouldReturnEmpty() {
        when(nonWorkingDaysRepo.findById(99L)).thenReturn(Optional.empty());

        var result = adapter.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_shouldReturnMappedList() {
        var entity = createEntity(1L);
        var page = new PageImpl<>(List.of(entity));
        when(nonWorkingDaysRepo.findByDateBetweenOrIsRecurring(date, date.plusDays(1), true, PageRequest.of(0, 10)))
                .thenReturn(page);

        var result = adapter.findAll(date, date.plusDays(1), true, 0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
    }

    @Test
    void count_withFilters_shouldReturnCount() {
        when(nonWorkingDaysRepo.countByDateBetweenOrIsRecurring(date, date.plusDays(1), true)).thenReturn(5L);

        var result = adapter.count(date, date.plusDays(1), true);

        assertThat(result).isEqualTo(5L);
    }

    @Test
    void count_withoutFilters_shouldReturnTotalCount() {
        when(nonWorkingDaysRepo.count()).thenReturn(10L);

        var result = adapter.count(null, null, null);

        assertThat(result).isEqualTo(10L);
    }

    @Test
    void existsByDate_shouldReturnTrueWhenExists() {
        when(nonWorkingDaysRepo.existsByDate(date)).thenReturn(true);

        var result = adapter.existsByDate(date);

        assertThat(result).isTrue();
    }

    @Test
    void existsByDate_shouldReturnFalseWhenNotExists() {
        when(nonWorkingDaysRepo.existsByDate(date)).thenReturn(false);

        var result = adapter.existsByDate(date);

        assertThat(result).isFalse();
    }

    @Test
    void deleteById_shouldDelegate() {
        adapter.deleteById(1L);

        verify(nonWorkingDaysRepo).deleteById(1L);
    }
}
