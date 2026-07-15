package trazzo.back.incidents.infrastructure.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import trazzo.back.incidents.domain.model.IncidentType;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentTypeEntity;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.repository.IncidentTypeSpringDataRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class IncidentTypeRepositoryAdapterTest {

    private IncidentTypeSpringDataRepository repository;
    private IncidentTypeRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = mock(IncidentTypeSpringDataRepository.class);
        adapter = new IncidentTypeRepositoryAdapter(repository);
    }

    @Test
    void save() {
        var now = LocalDateTime.now();
        var type = IncidentType.restore("1", "Permiso", "Desc", true, now, now);
        var entity = new IncidentTypeEntity(1, "Permiso", "Desc", true, now, now);
        when(repository.save(any())).thenReturn(entity);

        var result = adapter.save(type);

        assertNotNull(result);
        assertEquals("Permiso", result.getNombre());
    }

    @Test
    void findById() {
        var now = LocalDateTime.now();
        var entity = new IncidentTypeEntity(1, "Permiso", "Desc", true, now, now);
        when(repository.findById(1)).thenReturn(Optional.of(entity));

        var result = adapter.findById("1");

        assertTrue(result.isPresent());
        assertEquals("Permiso", result.get().getNombre());
    }

    @Test
    void findByIdNotFound() {
        when(repository.findById(999)).thenReturn(Optional.empty());
        assertTrue(adapter.findById("999").isEmpty());
    }

    @Test
    void findAllWithActivoFilter() {
        var now = LocalDateTime.now();
        var entity = new IncidentTypeEntity(1, "Permiso", "Desc", true, now, now);
        var page = new PageImpl<>(List.of(entity));
        when(repository.findByActivo(true, PageRequest.of(0, 10))).thenReturn(page);

        var results = adapter.findAll(true, 0, 10);

        assertEquals(1, results.size());
    }

    @Test
    void findAllWithoutFilter() {
        var now = LocalDateTime.now();
        var entity = new IncidentTypeEntity(1, "Permiso", "Desc", true, now, now);
        var page = new PageImpl<>(List.of(entity));
        when(repository.findAll(PageRequest.of(0, 10))).thenReturn(page);

        var results = adapter.findAll(null, 0, 10);

        assertEquals(1, results.size());
    }

    @Test
    void countWithActivoFilter() {
        Page<IncidentTypeEntity> page = new PageImpl<>(List.of(), PageRequest.of(0, 1), 3);
        when(repository.findByActivo(true, PageRequest.of(0, 1))).thenReturn(page);

        var count = adapter.count(true);

        assertEquals(3, count);
    }

    @Test
    void countWithoutFilter() {
        when(repository.count()).thenReturn(5L);
        assertEquals(5, adapter.count(null));
    }

    @Test
    void existsByNombre() {
        when(repository.existsByNombre("Permiso")).thenReturn(true);
        assertTrue(adapter.existsByNombre("Permiso"));
    }

    @Test
    void findByIdIn() {
        var now = LocalDateTime.now();
        var entity = new IncidentTypeEntity(1, "Permiso", "Desc", true, now, now);
        when(repository.findByIdIn(List.of(1))).thenReturn(List.of(entity));

        var results = adapter.findByIdIn(List.of("1"));

        assertEquals(1, results.size());
        assertEquals("Permiso", results.get(0).getNombre());
    }

    @Test
    void findByIdInSkipsInvalidIds() {
        var now = LocalDateTime.now();
        var entity = new IncidentTypeEntity(1, "Permiso", "Desc", true, now, now);
        when(repository.findByIdIn(List.of(1))).thenReturn(List.of(entity));

        var results = adapter.findByIdIn(List.of("1", "not-a-number", ""));

        assertEquals(1, results.size());
    }
}
