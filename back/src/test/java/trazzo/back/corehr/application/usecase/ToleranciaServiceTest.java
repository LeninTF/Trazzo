package trazzo.back.corehr.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.command.CreateToleranciaCommand;
import trazzo.back.corehr.application.dto.command.PatchToleranciaCommand;
import trazzo.back.corehr.application.port.out.ScheduleRepositoryPort;
import trazzo.back.corehr.application.port.out.ToleranciaRepositoryPort;
import trazzo.back.corehr.domain.model.ToleranciaType;
import trazzo.back.corehr.domain.model.schedule.Schedule;
import trazzo.back.corehr.domain.model.schedule.Tolerancia;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

class ToleranciaServiceTest {

    private ToleranciaRepositoryPort toleranciaRepository;
    private ScheduleRepositoryPort scheduleRepository;
    private ToleranciaService service;

    @BeforeEach
    void setUp() {
        toleranciaRepository = mock(ToleranciaRepositoryPort.class);
        scheduleRepository = mock(ScheduleRepositoryPort.class);
        service = new ToleranciaService(toleranciaRepository, scheduleRepository);
    }

    @Test
    void createWithValidData() {
        var now = LocalDateTime.now();
        var schedule = Schedule.restore(1L, 1L, "Horario A", "Desc",
                LocalTime.of(8, 0), LocalTime.of(17, 0), now, now);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(toleranciaRepository.existsActiveByScheduleIdAndType(1L, ToleranciaType.ENTRADA)).thenReturn(false);
        when(toleranciaRepository.save(any())).thenAnswer(invocation -> invocation.<Tolerancia>getArgument(0));

        var command = new CreateToleranciaCommand("Tolerancia 1", ToleranciaType.ENTRADA, 15, "Desc");
        var result = service.create(1L, command);

        assertEquals("Tolerancia 1", result.name());
        assertEquals(ToleranciaType.ENTRADA, result.type());
        assertEquals(15, result.minutes());
        assertTrue(result.activo());
        verify(toleranciaRepository).save(any());
    }

    @Test
    void createWithDuplicateTypeThrowsException() {
        var now = LocalDateTime.now();
        var schedule = Schedule.restore(1L, 1L, "Horario A", "Desc",
                LocalTime.of(8, 0), LocalTime.of(17, 0), now, now);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(toleranciaRepository.existsActiveByScheduleIdAndType(1L, ToleranciaType.ENTRADA)).thenReturn(true);

        var command = new CreateToleranciaCommand("Tolerancia 1", ToleranciaType.ENTRADA, 15, "Desc");

        assertThrows(IllegalStateException.class, () -> service.create(1L, command));
        verify(toleranciaRepository, never()).save(any());
    }

    @Test
    void createWithScheduleNotFoundThrowsException() {
        when(scheduleRepository.findById(99L)).thenReturn(Optional.empty());

        var command = new CreateToleranciaCommand("Tolerancia 1", ToleranciaType.ENTRADA, 15, "Desc");

        assertThrows(IllegalArgumentException.class, () -> service.create(99L, command));
        verify(toleranciaRepository, never()).save(any());
    }

    @Test
    void findAllByScheduleIdReturnsPaginatedResults() {
        var now = LocalDateTime.now();
        var schedule = Schedule.restore(1L, 1L, "Horario A", "Desc",
                LocalTime.of(8, 0), LocalTime.of(17, 0), now, now);
        var tolerancia = Tolerancia.restore(1L, 1L, "Tolerancia 1", ToleranciaType.ENTRADA,
                15, "Desc", true, now, now);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(toleranciaRepository.findAllByScheduleId(1L, 0, 10)).thenReturn(List.of(tolerancia));
        when(toleranciaRepository.countByScheduleId(1L)).thenReturn(1L);

        var result = service.findAllByScheduleId(1L, 0, 10);

        assertEquals(1, result.content().size());
        assertEquals(0, result.page());
        assertEquals(1, result.totalElements());
    }

    @Test
    void findAllByScheduleIdThrowsExceptionWhenScheduleNotFound() {
        when(scheduleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.findAllByScheduleId(99L, 0, 10));
    }

    @Test
    void patchUpdatesMinutes() {
        var now = LocalDateTime.now();
        var tolerancia = Tolerancia.restore(1L, 1L, "Tolerancia 1", ToleranciaType.ENTRADA,
                15, "Desc", true, now, now);
        when(toleranciaRepository.findByScheduleIdAndId(1L, 1L)).thenReturn(Optional.of(tolerancia));
        when(toleranciaRepository.save(any())).thenAnswer(invocation -> invocation.<Tolerancia>getArgument(0));

        var command = new PatchToleranciaCommand(null, 30, null, null);
        var result = service.patch(1L, 1L, command);

        assertEquals(30, result.minutes());
    }

    @Test
    void patchDeactivatesTolerancia() {
        var now = LocalDateTime.now();
        var tolerancia = Tolerancia.restore(1L, 1L, "Tolerancia 1", ToleranciaType.ENTRADA,
                15, "Desc", true, now, now);
        when(toleranciaRepository.findByScheduleIdAndId(1L, 1L)).thenReturn(Optional.of(tolerancia));
        when(toleranciaRepository.save(any())).thenAnswer(invocation -> invocation.<Tolerancia>getArgument(0));

        var command = new PatchToleranciaCommand(null, null, null, false);
        var result = service.patch(1L, 1L, command);

        assertFalse(result.activo());
    }

    @Test
    void patchWithNotFoundIdThrowsException() {
        when(toleranciaRepository.findByScheduleIdAndId(1L, 99L)).thenReturn(Optional.empty());

        var command = new PatchToleranciaCommand("Nuevo", null, null, null);

        assertThrows(IllegalArgumentException.class, () -> service.patch(1L, 99L, command));
    }

    @Test
    void deleteByIdSuccess() {
        var now = LocalDateTime.now();
        var tolerancia = Tolerancia.restore(1L, 1L, "Tolerancia 1", ToleranciaType.ENTRADA,
                15, "Desc", true, now, now);
        when(toleranciaRepository.findByScheduleIdAndId(1L, 1L)).thenReturn(Optional.of(tolerancia));

        service.deleteById(1L, 1L);

        verify(toleranciaRepository).deleteById(1L);
    }

    @Test
    void deleteByIdThrowsExceptionWhenNotFound() {
        when(toleranciaRepository.findByScheduleIdAndId(1L, 99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.deleteById(1L, 99L));
        verify(toleranciaRepository, never()).deleteById(any());
    }
}
