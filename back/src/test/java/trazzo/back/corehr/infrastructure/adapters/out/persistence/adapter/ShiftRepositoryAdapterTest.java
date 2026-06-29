package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import trazzo.back.corehr.domain.model.schedule.Shift;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.ShiftEntity;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.mapper.ShiftMapper;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.repository.ShiftJpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShiftRepositoryAdapterTest {

    @Mock
    private ShiftJpaRepository shiftRepo;

    @InjectMocks
    private ShiftRepositoryAdapter adapter;

    @Test
    void shouldSave() {
        var now = LocalDateTime.now();
        var domain = Shift.restore(1L, "Morning", "desc", now, now);

        var entity = ShiftMapper.toEntity(domain);
        when(shiftRepo.save(any())).thenReturn(entity);

        var result = adapter.save(domain);

        assertEquals(domain.getId(), result.getId());
        verify(shiftRepo).save(any());
    }

    @Test
    void shouldFindById() {
        var now = LocalDateTime.now();
        var entity = new ShiftEntity();
        entity.setId(1L);
        entity.setName("Morning");
        entity.setDescription("desc");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        when(shiftRepo.findById(1L)).thenReturn(Optional.of(entity));

        var result = adapter.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
    }

    @Test
    void shouldReturnEmptyWhenNotFound() {
        when(shiftRepo.findById(99L)).thenReturn(Optional.empty());

        assertTrue(adapter.findById(99L).isEmpty());
    }

    @Test
    void shouldFindAll() {
        var now = LocalDateTime.now();
        var entity = new ShiftEntity();
        entity.setId(1L);
        entity.setName("Morning");
        entity.setDescription("desc");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(shiftRepo.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findAll(null, 0, 10, null);

        assertEquals(1, result.size());
    }

    @Test
    void shouldFindAllWithSearch() {
        var now = LocalDateTime.now();
        var entity = new ShiftEntity();
        entity.setId(1L);
        entity.setName("Morning");
        entity.setDescription("desc");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(shiftRepo.findByNameContainingIgnoreCase("Morn", pageable)).thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findAll("Morn", 0, 10, null);

        assertEquals(1, result.size());
    }

    @Test
    void shouldCount() {
        when(shiftRepo.count()).thenReturn(5L);

        assertEquals(5L, adapter.count(null));
    }

    @Test
    void shouldCountWithSearch() {
        when(shiftRepo.countByNameContainingIgnoreCase("Morn")).thenReturn(2L);

        assertEquals(2L, adapter.count("Morn"));
    }

    @Test
    void shouldCheckExistsByName() {
        when(shiftRepo.existsByName("Morning")).thenReturn(true);

        assertTrue(adapter.existsByName("Morning"));
    }

    @Test
    void shouldDeleteById() {
        adapter.deleteById(1L);

        verify(shiftRepo).deleteById(1L);
    }
}
