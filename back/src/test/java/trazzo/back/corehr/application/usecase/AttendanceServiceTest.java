package trazzo.back.corehr.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.command.PatchAttendanceCommand;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.port.out.AttendanceRepositoryPort;
import trazzo.back.corehr.application.port.out.EventPublisherPort;
import trazzo.back.corehr.domain.model.AttendanceState;
import trazzo.back.corehr.domain.model.attendance.Attendance;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class AttendanceServiceTest {

    private AttendanceRepositoryPort repository;
    private EventPublisherPort eventPublisher;
    private AttendanceService service;

    @BeforeEach
    void setUp() {
        repository = mock(AttendanceRepositoryPort.class);
        eventPublisher = mock(EventPublisherPort.class);
        service = new AttendanceService(repository, eventPublisher);
    }

    @Test
    void findByIdReturnsAttendance() {
        var now = LocalDateTime.now();
        var attendance = Attendance.restore("id-1", 1L, 10L, 100L, now, null, LocalDate.now(), 0, AttendanceState.PUNTUAL, now, now);
        when(repository.findById("id-1")).thenReturn(Optional.of(attendance));

        var result = service.findById("id-1");

        assertTrue(result.isPresent());
        assertEquals("id-1", result.get().id());
        assertEquals(AttendanceState.PUNTUAL, result.get().state());
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        when(repository.findById("not-found")).thenReturn(Optional.empty());
        assertTrue(service.findById("not-found").isEmpty());
    }

    @Test
    void findAllReturnsPaginatedResults() {
        var now = LocalDateTime.now();
        var attendance = Attendance.restore("id-1", 1L, 10L, 100L, now, null, LocalDate.now(), 0, AttendanceState.PUNTUAL, now, now);
        when(repository.findAll("scope", 1L, 2L, 3L, null, null, null, null, 0, 10, "asc")).thenReturn(List.of(attendance));
        when(repository.count("scope", 1L, 2L, 3L, null, null, null, null)).thenReturn(1L);

        var result = service.findAll("scope", 1L, 2L, 3L, null, null, null, null, 0, 10, "asc");

        assertEquals(1, result.content().size());
        assertEquals(0, result.page());
        assertEquals(1, result.totalElements());
    }

    @Test
    void correctUpdatesStateAndMinutes() {
        var now = LocalDateTime.now();
        var attendance = Attendance.restore("id-1", 1L, 10L, 100L, now, null, LocalDate.now(), 0, AttendanceState.PUNTUAL, now, now);
        when(repository.findById("id-1")).thenReturn(Optional.of(attendance));
        when(repository.save(any())).thenAnswer(invocation -> invocation.<Attendance>getArgument(0));

        var command = new PatchAttendanceCommand(null, null, AttendanceState.TARDANZA, 15);
        var result = service.correct("id-1", command);

        assertEquals(AttendanceState.TARDANZA, result.state());
        assertEquals(15, result.minutesLate());
        verify(repository).findById("id-1");
        verify(repository).save(any());
    }

    @Test
    void correctUpdatesCheckIn() {
        var now = LocalDateTime.now();
        var newCheckIn = now.plusHours(1);
        var attendance = Attendance.restore("id-1", 1L, 10L, 100L, now, null, LocalDate.now(), 0, AttendanceState.PUNTUAL, now, now);
        when(repository.findById("id-1")).thenReturn(Optional.of(attendance));
        when(repository.save(any())).thenAnswer(invocation -> invocation.<Attendance>getArgument(0));

        var command = new PatchAttendanceCommand(newCheckIn, null, null, null);
        var result = service.correct("id-1", command);

        assertEquals(newCheckIn, result.checkIn());
        assertFalse(result.updatedAt().isBefore(now));
        verify(repository).save(any());
    }

    @Test
    void correctUpdatesCheckOut() {
        var now = LocalDateTime.now();
        var checkOut = now.plusHours(8);
        var attendance = Attendance.restore("id-1", 1L, 10L, 100L, now, null, LocalDate.now(), 0, AttendanceState.PUNTUAL, now, now);
        when(repository.findById("id-1")).thenReturn(Optional.of(attendance));
        when(repository.save(any())).thenAnswer(invocation -> invocation.<Attendance>getArgument(0));

        var command = new PatchAttendanceCommand(null, checkOut, null, null);
        var result = service.correct("id-1", command);

        assertEquals(checkOut, result.checkOut());
        verify(repository).save(any());
    }

    @Test
    void correctRejectsCheckOutBeforeCheckIn() {
        var now = LocalDateTime.now();
        var attendance = Attendance.restore("id-1", 1L, 10L, 100L, now, null, LocalDate.now(), 0, AttendanceState.PUNTUAL, now, now);
        when(repository.findById("id-1")).thenReturn(Optional.of(attendance));

        var command = new PatchAttendanceCommand(null, now.minusHours(1), null, null);

        assertThrows(trazzo.back.corehr.domain.exception.InvalidAttendanceException.class,
                () -> service.correct("id-1", command));
        verify(repository, never()).save(any());
    }

    @Test
    void correctWithNotFoundIdThrowsException() {
        when(repository.findById("bad-id")).thenReturn(Optional.empty());
        var command = new PatchAttendanceCommand(null, null, AttendanceState.PUNTUAL, 0);

        assertThrows(IllegalArgumentException.class, () -> service.correct("bad-id", command));
        verify(repository, never()).save(any());
    }

    @Test
    void correctPublishesDomainEvents() {
        var now = LocalDateTime.now();
        var attendance = Attendance.restore("id-1", 1L, 10L, 100L, now, null, LocalDate.now(), 0, AttendanceState.PUNTUAL, now, now);
        when(repository.findById("id-1")).thenReturn(Optional.of(attendance));
        when(repository.save(any())).thenAnswer(invocation -> invocation.<Attendance>getArgument(0));

        var command = new PatchAttendanceCommand(null, null, AttendanceState.TARDANZA, 10);
        service.correct("id-1", command);

        verify(eventPublisher, atLeastOnce()).publish(any());
    }
}
