package trazzo.back.corehr.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.command.CreateScheduleCommand;
import trazzo.back.corehr.application.dto.command.PatchScheduleCommand;
import trazzo.back.corehr.application.port.out.ScheduleRepositoryPort;
import trazzo.back.corehr.application.port.out.ShiftRepositoryPort;
import trazzo.back.corehr.application.port.out.ToleranciaRepositoryPort;
import trazzo.back.corehr.application.port.out.UserScheduleRepositoryPort;
import trazzo.back.corehr.domain.model.schedule.Schedule;
import trazzo.back.corehr.domain.model.schedule.Shift;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

class ScheduleServiceTest {

    private ScheduleRepositoryPort scheduleRepository;
    private ShiftRepositoryPort shiftRepository;
    private ToleranciaRepositoryPort toleranciaRepository;
    private UserScheduleRepositoryPort userScheduleRepository;
    private ScheduleService service;

    @BeforeEach
    void setUp() {
        scheduleRepository = mock(ScheduleRepositoryPort.class);
        shiftRepository = mock(ShiftRepositoryPort.class);
        toleranciaRepository = mock(ToleranciaRepositoryPort.class);
        userScheduleRepository = mock(UserScheduleRepositoryPort.class);
        service = new ScheduleService(scheduleRepository, shiftRepository, toleranciaRepository, userScheduleRepository);
    }

    @Test
    void createWithValidData() {
        var now = LocalDateTime.now();
        var shift = Shift.restore(1L, "Turno Matutino", "Desc", now, now);
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(shift));
        when(scheduleRepository.save(any())).thenAnswer(invocation -> invocation.<Schedule>getArgument(0));

        var command = new CreateScheduleCommand(1L, "Horario A", "Desc", LocalTime.of(8, 0), LocalTime.of(17, 0));
        var result = service.create(command);

        assertEquals("Horario A", result.name());
        assertEquals(1L, result.shiftId());
        verify(scheduleRepository).save(any());
    }

    @Test
    void createWithShiftNotFoundThrowsException() {
        when(shiftRepository.findById(99L)).thenReturn(Optional.empty());

        var command = new CreateScheduleCommand(99L, "Horario A", "Desc", LocalTime.of(8, 0), LocalTime.of(17, 0));

        assertThrows(IllegalArgumentException.class, () -> service.create(command));
        verify(scheduleRepository, never()).save(any());
    }

    @Test
    void findByIdReturnsSchedule() {
        var now = LocalDateTime.now();
        var schedule = Schedule.restore(1L, 1L, "Horario A", "Desc",
                LocalTime.of(8, 0), LocalTime.of(17, 0), now, now);
        var shift = Shift.restore(1L, "Turno Matutino", "Desc", now, now);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(shift));
        when(toleranciaRepository.findAllByScheduleId(1L, 0, 100)).thenReturn(List.of());

        var result = service.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("Horario A", result.get().name());
        assertEquals("Turno Matutino", result.get().shift().name());
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        when(scheduleRepository.findById(99L)).thenReturn(Optional.empty());

        assertTrue(service.findById(99L).isEmpty());
    }

    @Test
    void findAllReturnsPaginatedResults() {
        var now = LocalDateTime.now();
        var schedule = Schedule.restore(1L, 1L, "Horario A", "Desc",
                LocalTime.of(8, 0), LocalTime.of(17, 0), now, now);
        var shift = Shift.restore(1L, "Turno Matutino", "Desc", now, now);
        when(scheduleRepository.findAll(1L, 0, 10, "name")).thenReturn(List.of(schedule));
        when(scheduleRepository.count(1L)).thenReturn(1L);
        when(shiftRepository.findById(1L)).thenReturn(Optional.of(shift));

        var result = service.findAll(1L, 0, 10, "name");

        assertEquals(1, result.content().size());
        assertEquals(0, result.page());
        assertEquals(1, result.totalElements());
    }

    @Test
    void patchUpdatesName() {
        var now = LocalDateTime.now();
        var schedule = Schedule.restore(1L, 1L, "Original", "Desc",
                LocalTime.of(8, 0), LocalTime.of(17, 0), now, now);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any())).thenAnswer(invocation -> invocation.<Schedule>getArgument(0));

        var command = new PatchScheduleCommand("Modificado", null, null, null);
        var result = service.patch(1L, command);

        assertEquals("Modificado", result.name());
    }

    @Test
    void patchUpdatesTimes() {
        var now = LocalDateTime.now();
        var schedule = Schedule.restore(1L, 1L, "Original", "Desc",
                LocalTime.of(8, 0), LocalTime.of(17, 0), now, now);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any())).thenAnswer(invocation -> invocation.<Schedule>getArgument(0));

        var command = new PatchScheduleCommand(null, null, LocalTime.of(9, 0), LocalTime.of(18, 0));
        var result = service.patch(1L, command);

        assertEquals(LocalTime.of(9, 0), result.entryTime());
        assertEquals(LocalTime.of(18, 0), result.departureTime());
    }

    @Test
    void patchWithNotFoundIdThrowsException() {
        when(scheduleRepository.findById(99L)).thenReturn(Optional.empty());

        var command = new PatchScheduleCommand("Nuevo", null, null, null);

        assertThrows(IllegalArgumentException.class, () -> service.patch(99L, command));
    }

    @Test
    void deleteByIdSuccess() {
        var now = LocalDateTime.now();
        var schedule = Schedule.restore(1L, 1L, "Horario A", "Desc",
                LocalTime.of(8, 0), LocalTime.of(17, 0), now, now);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(userScheduleRepository.count(null, 1L)).thenReturn(0L);

        service.deleteById(1L);

        verify(scheduleRepository).deleteById(1L);
    }

    @Test
    void deleteByIdThrowsExceptionWhenHasActiveDependencies() {
        var now = LocalDateTime.now();
        var schedule = Schedule.restore(1L, 1L, "Horario A", "Desc",
                LocalTime.of(8, 0), LocalTime.of(17, 0), now, now);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(userScheduleRepository.count(null, 1L)).thenReturn(3L);

        assertThrows(IllegalStateException.class, () -> service.deleteById(1L));
        verify(scheduleRepository, never()).deleteById(any());
    }

    @Test
    void deleteByIdThrowsExceptionWhenNotFound() {
        when(scheduleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.deleteById(99L));
        verify(scheduleRepository, never()).deleteById(any());
    }
}
