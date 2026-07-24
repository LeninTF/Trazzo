package trazzo.back.incidents.infrastructure.adapters.out.persistence;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import trazzo.back.incidents.domain.model.Incident;
import trazzo.back.incidents.domain.model.IncidentState;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentEntity;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.entity.IncidentPermissionEntity;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.repository.IncidentEvidenceSpringDataRepository;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.repository.IncidentPermissionSpringDataRepository;
import trazzo.back.incidents.infrastructure.adapters.out.persistence.repository.IncidentSpringDataRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class IncidentRepositoryAdapterTest {

    private IncidentSpringDataRepository incidentRepo;
    private IncidentEvidenceSpringDataRepository evidenceRepo;
    private IncidentPermissionSpringDataRepository permissionRepo;
    private IncidentRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        incidentRepo = mock(IncidentSpringDataRepository.class);
        evidenceRepo = mock(IncidentEvidenceSpringDataRepository.class);
        permissionRepo = mock(IncidentPermissionSpringDataRepository.class);
        adapter = new IncidentRepositoryAdapter(incidentRepo, evidenceRepo, permissionRepo);
    }

    @Test
    void saveWithoutPermission() {
        var now = LocalDateTime.now();
        var incident = Incident.restore("1", "u-1", "t-1", IncidentState.PENDIENTE,
                "comment", null, null, null, List.of(), now, now);
        var entity = new IncidentEntity(1, 1, 1, IncidentState.PENDIENTE,
                "comment", null, now, now, List.of(), null);
        when(incidentRepo.save(any())).thenReturn(entity);
        when(incidentRepo.findById(1)).thenReturn(Optional.of(entity));
        when(permissionRepo.findByIncidentId(1)).thenReturn(Optional.empty());

        var saved = adapter.save(incident);

        assertEquals("1", saved.getId());
        verify(permissionRepo, never()).save(any());
    }

    @Test
    void saveWithPermission() {
        var now = LocalDateTime.now();
        var permission = trazzo.back.incidents.domain.model.IncidentPermission.create("1",
                java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(1), 1);
        var incident = Incident.restore("1", "u-1", "t-1", IncidentState.PENDIENTE,
                "comment", null, null, permission, List.of(), now, now);
        var entity = new IncidentEntity(1, 1, 1, IncidentState.PENDIENTE,
                "comment", null, now, now, List.of(), null);
        var permEntity = new IncidentPermissionEntity(1, 1,
                java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(1), 1, now, now);
        when(incidentRepo.save(any())).thenReturn(entity);
        when(incidentRepo.findById(1)).thenReturn(Optional.of(entity));
        when(permissionRepo.findByIncidentId(1)).thenReturn(Optional.of(permEntity));

        adapter.save(incident);

        verify(permissionRepo).save(any());
    }

    @Test
    void findByIdWhenNotFound() {
        when(incidentRepo.findById(999)).thenReturn(Optional.empty());
        assertTrue(adapter.findById("999").isEmpty());
    }

    @Test
    void findByIdWithPermission() {
        var now = LocalDateTime.now();
        var entity = new IncidentEntity(1, 1, 1, IncidentState.PENDIENTE,
                "comment", null, now, now, List.of(), null);
        var permEntity = new IncidentPermissionEntity(1, 1,
                java.time.LocalDate.now(), java.time.LocalDate.now().plusDays(1), 1, now, now);
        when(incidentRepo.findById(1)).thenReturn(Optional.of(entity));
        when(permissionRepo.findByIncidentId(1)).thenReturn(Optional.of(permEntity));

        var result = adapter.findById("1");

        assertTrue(result.isPresent());
        assertNotNull(result.get().getPermission());
    }

    @Test
    void findAllWithFilters() {
        var now = LocalDateTime.now();
        var entity = new IncidentEntity(1, 1, 1, IncidentState.PENDIENTE,
                "comment", null, now, now, List.of(), null);
        var page = new PageImpl<>(List.of(entity));
        when(incidentRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        var results = adapter.findAll("1", "PENDIENTE", null, null, null, null, 0, 20, null);

        assertEquals(1, results.size());
    }

    @Test
    void findAllWithoutFilters() {
        var now = LocalDateTime.now();
        var entity = new IncidentEntity(1, 1, 1, IncidentState.PENDIENTE,
                "comment", null, now, now, List.of(), null);
        var page = new PageImpl<>(List.of(entity));
        when(incidentRepo.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        var results = adapter.findAll(null, null, null, null, null, null, 0, 20, null);

        assertEquals(1, results.size());
    }

    @Test
    void countWithFilters() {
        when(incidentRepo.count(any(Specification.class))).thenReturn(5L);

        var count = adapter.count("1", null, null, null, null, null);

        assertEquals(5, count);
    }

    @Test
    void countWithoutFilters() {
        when(incidentRepo.count(any(Specification.class))).thenReturn(10L);

        var count = adapter.count(null, null, null, null, null, null);

        assertEquals(10, count);
    }

    @Test
    void deleteById() {
        adapter.deleteById("1");

        verify(evidenceRepo).deleteByIncidentId(1);
        verify(permissionRepo).deleteByIncidentId(1);
        verify(incidentRepo).deleteById(1);
    }

    @Test
    void parseSortWithBlankDefaultsToDescCreatedAt() {
        when(incidentRepo.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(new PageImpl<>(List.of()));
        adapter.findAll(null, null, null, null, null, null, 0, 20, null);
        verify(incidentRepo).findAll(any(Specification.class),
                eq(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @Test
    void parseSortWithFieldAndAscDirection() {
        when(incidentRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        adapter.findAll("1", null, null, null, null, null, 0, 20, "state,asc");
        verify(incidentRepo).findAll(any(Specification.class),
                eq(PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "state"))));
    }

    @Test
    void parseSortWithFieldAndDescDirection() {
        when(incidentRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        adapter.findAll("1", null, null, null, null, null, 0, 20, "updatedAt,desc");
        verify(incidentRepo).findAll(any(Specification.class),
                eq(PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "updatedAt"))));
    }

    @Test
    void parseSortMapsCreatedAtField() {
        when(incidentRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        adapter.findAll("1", null, null, null, null, null, 0, 20, "created_at,asc");
        verify(incidentRepo).findAll(any(Specification.class),
                eq(PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "createdAt"))));
    }

    @Test
    void findAllWithSearchUsesSearchQuery() {
        var now = LocalDateTime.now();
        var entity = new IncidentEntity(1, 1, 1, IncidentState.PENDIENTE,
                "comment", null, now, now, List.of(), null);
        var page = new PageImpl<>(List.of(entity));
        when(incidentRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        var results = adapter.findAll("1", "PENDIENTE", null, null, null, "comm", 0, 20, null);

        assertEquals(1, results.size());
        verify(incidentRepo).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void countWithSearchUsesSearchQuery() {
        when(incidentRepo.count(any(Specification.class))).thenReturn(3L);

        var count = adapter.count("1", null, null, null, null, "term");

        assertEquals(3, count);
        verify(incidentRepo).count(any(Specification.class));
    }
}
