package trazzo.back.corehr.infrastructure.adapters.out.persistence.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import trazzo.back.corehr.domain.model.ToleranciaType;
import trazzo.back.corehr.domain.model.schedule.Tolerancia;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.entity.ToleranciaEntity;
import trazzo.back.corehr.infrastructure.adapters.out.persistence.repository.ToleranciaJpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToleranciaRepositoryAdapterTest {

    @Mock
    private ToleranciaJpaRepository toleranciaRepo;

    @InjectMocks
    private ToleranciaRepositoryAdapter adapter;

    private final LocalDateTime now = LocalDateTime.now();

    private ToleranciaEntity createEntity(Long id) {
        var e = new ToleranciaEntity();
        e.setId(id);
        e.setScheduleId(10L);
        e.setName("Tolerance 1");
        e.setType(ToleranciaType.ENTRADA);
        e.setMinutes(15);
        e.setDescription("Some tolerance");
        e.setActivo(true);
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        return e;
    }

    @Test
    void save_shouldPersistAndReturnDomain() {
        var domain = Tolerancia.restore(null, 10L, "Tolerance 1", ToleranciaType.ENTRADA, 15, "desc", true, now, now);
        var entity = createEntity(1L);
        when(toleranciaRepo.save(any())).thenReturn(entity);

        var result = adapter.save(domain);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getScheduleId()).isEqualTo(10L);
        assertThat(result.getMinutes()).isEqualTo(15);
        verify(toleranciaRepo).save(any());
    }

    @Test
    void findById_whenExists_shouldReturnDomain() {
        var entity = createEntity(1L);
        when(toleranciaRepo.findById(1L)).thenReturn(Optional.of(entity));

        var result = adapter.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void findById_whenNotExists_shouldReturnEmpty() {
        when(toleranciaRepo.findById(99L)).thenReturn(Optional.empty());

        var result = adapter.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void findAllByScheduleId_shouldReturnMappedList() {
        var entity = createEntity(1L);
        var pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(toleranciaRepo.findByScheduleId(10L, pageable)).thenReturn(new PageImpl<>(List.of(entity)));

        var result = adapter.findAllByScheduleId(10L, 0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScheduleId()).isEqualTo(10L);
    }

    @Test
    void countByScheduleId_shouldReturnCount() {
        when(toleranciaRepo.countByScheduleId(10L)).thenReturn(3L);

        var result = adapter.countByScheduleId(10L);

        assertThat(result).isEqualTo(3L);
    }

    @Test
    void existsActiveByScheduleIdAndType_shouldReturnTrueWhenExists() {
        when(toleranciaRepo.existsByScheduleIdAndTypeAndActivoTrue(10L, ToleranciaType.ENTRADA)).thenReturn(true);

        var result = adapter.existsActiveByScheduleIdAndType(10L, ToleranciaType.ENTRADA);

        assertThat(result).isTrue();
    }

    @Test
    void existsActiveByScheduleIdAndType_shouldReturnFalseWhenNotExists() {
        when(toleranciaRepo.existsByScheduleIdAndTypeAndActivoTrue(10L, ToleranciaType.ENTRADA)).thenReturn(false);

        var result = adapter.existsActiveByScheduleIdAndType(10L, ToleranciaType.ENTRADA);

        assertThat(result).isFalse();
    }

    @Test
    void deleteById_shouldDelegate() {
        adapter.deleteById(1L);

        verify(toleranciaRepo).deleteById(1L);
    }

    @Test
    void findByScheduleIdAndId_whenExists_shouldReturnDomain() {
        var entity = createEntity(1L);
        when(toleranciaRepo.findByScheduleIdAndId(10L, 1L)).thenReturn(Optional.of(entity));

        var result = adapter.findByScheduleIdAndId(10L, 1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
    }

    @Test
    void findByScheduleIdAndId_whenNotExists_shouldReturnEmpty() {
        when(toleranciaRepo.findByScheduleIdAndId(99L, 99L)).thenReturn(Optional.empty());

        var result = adapter.findByScheduleIdAndId(99L, 99L);

        assertThat(result).isEmpty();
    }
}
