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
import org.springframework.data.domain.Sort;
import trazzo.back.corehr.domain.model.schedule.Shift;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.ShiftEntity;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.repository.ShiftJpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShiftRepositoryAdapterTest {

    @Mock ShiftJpaRepository shiftRepo;
    @InjectMocks ShiftRepositoryAdapter adapter;

    @Captor ArgumentCaptor<ShiftEntity> entityCaptor;

    private static final LocalDateTime NOW = LocalDateTime.now();

    private Shift domain() {
        return Shift.restore(1L, "Morning", "desc", NOW, NOW);
    }

    private ShiftEntity entity() {
        var e = new ShiftEntity();
        e.setId(1L);
        e.setName("Morning");
        e.setDescription("desc");
        e.setCreatedAt(NOW);
        e.setUpdatedAt(NOW);
        return e;
    }

    @Test
    void save_shouldPersistAndReturnDomain() {
        var domain = domain();
        when(shiftRepo.save(any(ShiftEntity.class))).thenReturn(entity());

        var result = adapter.save(domain);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Morning");
        verify(shiftRepo).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getName()).isEqualTo("Morning");
    }

    @Test
    void findById_shouldReturnDomainWhenFound() {
        when(shiftRepo.findById(1L)).thenReturn(Optional.of(entity()));

        var result = adapter.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Morning");
    }

    @Test
    void findById_shouldReturnEmptyWhenNotFound() {
        when(shiftRepo.findById(99L)).thenReturn(Optional.empty());

        assertThat(adapter.findById(99L)).isEmpty();
    }

    @Test
    void findAll_withoutSearch_returnsAll() {
        when(shiftRepo.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(entity())));

        var result = adapter.findAll(null, 0, 20, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Morning");
    }

    @Test
    void findAll_withSearch_filtersByName() {
        when(shiftRepo.findByNameContainingIgnoreCase(eq("Morning"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(entity())));

        var result = adapter.findAll("Morning", 0, 20, "name,asc");

        assertThat(result).hasSize(1);
    }

    @Test
    void findAll_withSort_appliesSort() {
        when(shiftRepo.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        adapter.findAll(null, 0, 20, "name,desc");
        var captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(shiftRepo).findAll(captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("name").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void findAll_withSortByCreatedAt_shouldApplySort() {
        when(shiftRepo.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        adapter.findAll(null, 0, 20, "createdAt,desc");

        var captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(shiftRepo).findAll(captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void findAll_withSortByCreatedAtSnake_shouldApplySort() {
        when(shiftRepo.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        adapter.findAll(null, 0, 20, "created_at,asc");

        var captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(shiftRepo).findAll(captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void findAll_withSortByUpdatedAt_shouldApplySort() {
        when(shiftRepo.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        adapter.findAll(null, 0, 20, "updatedAt");

        var captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(shiftRepo).findAll(captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("updatedAt")).isNotNull();
    }

    @Test
    void findAll_withSortByUpdatedAtSnake_shouldApplySort() {
        when(shiftRepo.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        adapter.findAll(null, 0, 20, "updated_at,desc");

        var captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(shiftRepo).findAll(captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("updatedAt")).isNotNull();
    }

    @Test
    void findAll_withUnknownSortField_shouldDefaultToCreatedAt() {
        when(shiftRepo.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));

        adapter.findAll(null, 0, 20, "unknownField,asc");

        var captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(shiftRepo).findAll(captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("createdAt")).isNotNull();
    }

    @Test
    void findAll_withBlankSearch_shouldReturnAll() {
        when(shiftRepo.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(entity())));

        var result = adapter.findAll("  ", 0, 20, null);

        assertThat(result).hasSize(1);
        verify(shiftRepo).findAll(any(PageRequest.class));
        verify(shiftRepo, never()).findByNameContainingIgnoreCase(anyString(), any());
    }

    @Test
    void count_withoutSearch_returnsAll() {
        when(shiftRepo.count()).thenReturn(10L);
        assertThat(adapter.count(null)).isEqualTo(10L);
    }

    @Test
    void count_withSearch_filters() {
        when(shiftRepo.countByNameContainingIgnoreCase("Morning")).thenReturn(3L);
        assertThat(adapter.count("Morning")).isEqualTo(3L);
    }

    @Test
    void existsByName_delegates() {
        when(shiftRepo.existsByName("Morning")).thenReturn(true);
        assertThat(adapter.existsByName("Morning")).isTrue();
    }

    @Test
    void deleteById_delegates() {
        adapter.deleteById(1L);
        verify(shiftRepo).deleteById(1L);
    }
}
