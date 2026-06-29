package trazzo.back.corehr.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.port.out.UserBiometriaRepositoryPort;
import trazzo.back.corehr.domain.model.attendance.UserBiometria;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class UserBiometriaServiceTest {

    private UserBiometriaRepositoryPort repository;
    private UserBiometriaService service;

    @BeforeEach
    void setUp() {
        repository = mock(UserBiometriaRepositoryPort.class);
        service = new UserBiometriaService(repository);
    }

    @Test
    void findAllWithFilters() {
        var now = LocalDateTime.now();
        var biometria = UserBiometria.restore(1L, 10L, 100L, 1, "template", "llave", now, true, now, now);
        when(repository.findAll(10L, 100L, true, 0, 10)).thenReturn(List.of(biometria));
        when(repository.count(10L, 100L, true)).thenReturn(1L);

        PaginatedResult<?> result = service.findAll(10L, 100L, true, 0, 10);

        assertEquals(1, result.content().size());
        assertEquals(0, result.page());
        assertEquals(1, result.totalElements());
        verify(repository).findAll(10L, 100L, true, 0, 10);
        verify(repository).count(10L, 100L, true);
    }

    @Test
    void findAllWithNullFilters() {
        var now = LocalDateTime.now();
        var biometria = UserBiometria.restore(1L, 10L, 100L, 1, "template", "llave", now, true, now, now);
        when(repository.findAll(null, null, null, 0, 10)).thenReturn(List.of(biometria));
        when(repository.count(null, null, null)).thenReturn(1L);

        PaginatedResult<?> result = service.findAll(null, null, null, 0, 10);

        assertEquals(1, result.content().size());
        verify(repository).findAll(null, null, null, 0, 10);
    }

    @Test
    void patchActivoActivates() {
        var now = LocalDateTime.now();
        var biometria = UserBiometria.restore(1L, 10L, 100L, 1, "template", "llave", now, false, now, now);
        when(repository.findById(1L)).thenReturn(Optional.of(biometria));
        when(repository.save(any())).thenAnswer(invocation -> invocation.<UserBiometria>getArgument(0));

        var result = service.patchActivo(1L, true);

        assertTrue(result.activo());
        verify(repository).findById(1L);
        verify(repository).save(biometria);
    }

    @Test
    void patchActivoDeactivates() {
        var now = LocalDateTime.now();
        var biometria = UserBiometria.restore(1L, 10L, 100L, 1, "template", "llave", now, true, now, now);
        when(repository.findById(1L)).thenReturn(Optional.of(biometria));
        when(repository.save(any())).thenAnswer(invocation -> invocation.<UserBiometria>getArgument(0));

        var result = service.patchActivo(1L, false);

        assertFalse(result.activo());
        verify(repository).findById(1L);
        verify(repository).save(biometria);
    }

    @Test
    void patchActivoWithNotFoundIdThrowsException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        var ex = assertThrows(IllegalArgumentException.class, () -> service.patchActivo(99L, true));
        assertTrue(ex.getMessage().contains("no encontrado"));
        verify(repository).findById(99L);
        verify(repository, never()).save(any());
    }
}
