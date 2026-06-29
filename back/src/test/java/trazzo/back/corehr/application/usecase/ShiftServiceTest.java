package trazzo.back.corehr.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.command.CreateShiftCommand;
import trazzo.back.corehr.application.dto.command.PatchShiftCommand;
import trazzo.back.corehr.application.port.out.ScheduleRepositoryPort;
import trazzo.back.corehr.application.port.out.ShiftRepositoryPort;
import trazzo.back.corehr.domain.model.schedule.Shift;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class ShiftServiceTest {

    private ShiftRepositoryPort shiftRepository;
    private ScheduleRepositoryPort scheduleRepository;
    private ShiftService service;

    @BeforeEach
    void setUp() {
        shiftRepository = mock(ShiftRepositoryPort.class);
        scheduleRepository = mock(ScheduleRepositoryPort.class);
        service = new ShiftService(shiftRepository, scheduleRepository);
    }

    @Test
    void createWithValidData() {
        when(shiftRepository.existsByName("Turno Matutino")).thenReturn(false);
        when(shiftRepository.save(any())).thenAnswer(invocation -> invocation.<Shift>getArgument(0));

        var command = new CreateShiftCommand("Turno Matutino", "Descripción");
        var result = service.create(command);

        assertEquals("Turno Matutino", result.name());
        assertEquals("Descripción", result.description());
        verify(shiftRepository).save(any());
    }

    @Test
    void createWithDuplicateNameThrowsException() {
        when(shiftRepository.existsByName("Turno Matutino")).thenReturn(true);

        var command = new CreateShiftCommand("Turno Matutino", "Desc");

        assertThrows(IllegalArgumentException.class, () -> service.create(command));
        verify(shiftRepository, never()).save(any());
    }

    @Test
    void findByIdReturnsShift() {
        var now = LocalDateTime.now();
        var shift = Shift.restore(1L, "Turno Matutino", "Desc", now, now);
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(shift));

        var result = service.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("Turno Matutino", result.get().name());
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        when(shiftRepository.findById(99L)).thenReturn(Optional.empty());

        assertTrue(service.findById(99L).isEmpty());
    }

    @Test
    void findAllReturnsPaginatedResults() {
        var now = LocalDateTime.now();
        var shift = Shift.restore(1L, "Turno Matutino", "Desc", now, now);
        when(shiftRepository.findAll("Matutino", 0, 10, "name")).thenReturn(List.of(shift));
        when(shiftRepository.count("Matutino")).thenReturn(1L);

        var result = service.findAll("Matutino", 0, 10, "name");

        assertEquals(1, result.content().size());
        assertEquals(0, result.page());
        assertEquals(1, result.totalElements());
    }

    @Test
    void patchUpdatesName() {
        var now = LocalDateTime.now();
        var shift = Shift.restore(1L, "Original", "Desc", now, now);
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(shift));
        when(shiftRepository.save(any())).thenAnswer(invocation -> invocation.<Shift>getArgument(0));

        var command = new PatchShiftCommand("Modificado", null);
        var result = service.patch(1L, command);

        assertEquals("Modificado", result.name());
        assertEquals("Desc", result.description());
    }

    @Test
    void patchUpdatesDescription() {
        var now = LocalDateTime.now();
        var shift = Shift.restore(1L, "Original", "Desc", now, now);
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(shift));
        when(shiftRepository.save(any())).thenAnswer(invocation -> invocation.<Shift>getArgument(0));

        var command = new PatchShiftCommand(null, "Nueva desc");
        var result = service.patch(1L, command);

        assertEquals("Nueva desc", result.description());
    }

    @Test
    void patchWithNotFoundIdThrowsException() {
        when(shiftRepository.findById(99L)).thenReturn(Optional.empty());

        var command = new PatchShiftCommand("Nuevo", null);

        assertThrows(IllegalArgumentException.class, () -> service.patch(99L, command));
    }

    @Test
    void deleteByIdSuccess() {
        var now = LocalDateTime.now();
        var shift = Shift.restore(1L, "Turno Matutino", "Desc", now, now);
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(shift));
        when(scheduleRepository.countActiveSchedulesByShiftId(1L)).thenReturn(0L);

        service.deleteById(1L);

        verify(shiftRepository).deleteById(1L);
    }

    @Test
    void deleteByIdThrowsExceptionWhenHasActiveSchedules() {
        var now = LocalDateTime.now();
        var shift = Shift.restore(1L, "Turno Matutino", "Desc", now, now);
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(shift));
        when(scheduleRepository.countActiveSchedulesByShiftId(1L)).thenReturn(5L);

        assertThrows(IllegalStateException.class, () -> service.deleteById(1L));
        verify(shiftRepository, never()).deleteById(any());
    }

    @Test
    void deleteByIdThrowsExceptionWhenNotFound() {
        when(shiftRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.deleteById(99L));
        verify(shiftRepository, never()).deleteById(any());
    }
}
