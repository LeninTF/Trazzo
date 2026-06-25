package trazzo.back.incidents.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.incidents.application.dto.command.CreateIncidentTypeCommand;
import trazzo.back.incidents.application.dto.command.PatchIncidentTypeCommand;
import trazzo.back.incidents.application.dto.result.IncidentTypeResult;
import trazzo.back.incidents.application.port.out.IncidentTypeRepositoryPort;
import trazzo.back.incidents.domain.model.IncidentType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class IncidentTypeServiceTest {

    private IncidentTypeRepositoryPort repository;
    private IncidentTypeService service;

    @BeforeEach
    void setUp() {
        repository = mock(IncidentTypeRepositoryPort.class);
        service = new IncidentTypeService(repository);
    }

    @Test
    void createWithUniqueName() {
        when(repository.existsByNombre("Permiso")).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.<IncidentType>getArgument(0));

        var command = new CreateIncidentTypeCommand("Permiso", "Desc");
        var result = service.create(command);

        assertEquals("Permiso", result.nombre());
        assertEquals("Desc", result.descripcion());
        assertTrue(result.activo());
        verify(repository).save(any());
    }

    @Test
    void createWithDuplicateNameThrowsException() {
        when(repository.existsByNombre("Permiso")).thenReturn(true);
        var command = new CreateIncidentTypeCommand("Permiso", "Desc");

        assertThrows(IllegalArgumentException.class, () -> service.create(command));
        verify(repository, never()).save(any());
    }

    @Test
    void findByIdReturnsType() {
        var now = LocalDateTime.now();
        var type = IncidentType.restore("id-1", "Permiso", "Desc", true, now, now);
        when(repository.findById("id-1")).thenReturn(Optional.of(type));

        var result = service.findById("id-1");

        assertTrue(result.isPresent());
        assertEquals("Permiso", result.get().nombre());
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        when(repository.findById("not-found")).thenReturn(Optional.empty());
        assertTrue(service.findById("not-found").isEmpty());
    }

    @Test
    void findAllReturnsPaginatedResults() {
        var now = LocalDateTime.now();
        var type = IncidentType.restore("id-1", "Permiso", "Desc", true, now, now);
        when(repository.findAll(true, 0, 10)).thenReturn(List.of(type));
        when(repository.count(true)).thenReturn(1L);

        var result = service.findAll(true, 0, 10);

        assertEquals(1, result.content().size());
        assertEquals(0, result.page());
        assertEquals(1, result.totalElements());
    }

    @Test
    void patchUpdatesNombre() {
        var now = LocalDateTime.now();
        var type = IncidentType.restore("id-1", "Original", "Desc", true, now, now);
        when(repository.findById("id-1")).thenReturn(Optional.of(type));
        when(repository.save(any())).thenAnswer(invocation -> invocation.<IncidentType>getArgument(0));

        var command = new PatchIncidentTypeCommand("Modificado", null, null);
        var result = service.patch("id-1", command);

        assertEquals("Modificado", result.nombre());
        assertEquals("Desc", result.descripcion());
    }

    @Test
    void patchDeactivatesType() {
        var now = LocalDateTime.now();
        var type = IncidentType.restore("id-1", "Permiso", "Desc", true, now, now);
        when(repository.findById("id-1")).thenReturn(Optional.of(type));
        when(repository.save(any())).thenAnswer(invocation -> invocation.<IncidentType>getArgument(0));

        var command = new PatchIncidentTypeCommand(null, null, false);
        var result = service.patch("id-1", command);

        assertFalse(result.activo());
    }

    @Test
    void patchWithNotFoundIdThrowsException() {
        when(repository.findById("bad-id")).thenReturn(Optional.empty());
        var command = new PatchIncidentTypeCommand("Nuevo", null, null);

        assertThrows(IllegalArgumentException.class, () -> service.patch("bad-id", command));
    }
}
