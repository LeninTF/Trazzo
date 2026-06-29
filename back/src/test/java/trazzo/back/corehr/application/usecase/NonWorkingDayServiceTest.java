package trazzo.back.corehr.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.command.CreateNonWorkingDayCommand;
import trazzo.back.corehr.application.dto.command.PatchNonWorkingDayCommand;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.port.out.NonWorkingDaysRepositoryPort;
import trazzo.back.corehr.domain.model.schedule.NonWorkingDays;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class NonWorkingDayServiceTest {

    private NonWorkingDaysRepositoryPort repository;
    private NonWorkingDayService service;

    @BeforeEach
    void setUp() {
        repository = mock(NonWorkingDaysRepositoryPort.class);
        service = new NonWorkingDayService(repository);
    }

    @Test
    void createWithValidData() {
        var date = LocalDate.of(2025, 12, 25);
        when(repository.existsByDate(date)).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> {
            var nwd = invocation.<NonWorkingDays>getArgument(0);
            return NonWorkingDays.restore(1L, nwd.getDate(), nwd.getDescription(), nwd.isRecurring(), nwd.getCreatedAt());
        });

        var command = new CreateNonWorkingDayCommand(date, "Navidad", true);
        var result = service.create(command);

        assertEquals(1L, result.id());
        assertEquals(date, result.date());
        assertEquals("Navidad", result.description());
        assertTrue(result.isRecurring());
        verify(repository).existsByDate(date);
        verify(repository).save(any());
    }

    @Test
    void createWithDuplicateDateThrowsException() {
        var date = LocalDate.of(2025, 12, 25);
        when(repository.existsByDate(date)).thenReturn(true);

        var command = new CreateNonWorkingDayCommand(date, "Navidad", true);
        assertThrows(IllegalArgumentException.class, () -> service.create(command));
        verify(repository, never()).save(any());
    }

    @Test
    void findByIdReturnsNonWorkingDay() {
        var date = LocalDate.of(2025, 12, 25);
        var now = LocalDateTime.now();
        var nwd = NonWorkingDays.restore(1L, date, "Navidad", true, now);
        when(repository.findById(1L)).thenReturn(Optional.of(nwd));

        var result = service.findById(1L);

        assertTrue(result.isPresent());
        assertEquals("Navidad", result.get().description());
    }

    @Test
    void findByIdReturnsEmptyWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertTrue(service.findById(99L).isEmpty());
    }

    @Test
    void findAllReturnsPaginatedResults() {
        var date = LocalDate.of(2025, 12, 25);
        var now = LocalDateTime.now();
        var nwd = NonWorkingDays.restore(1L, date, "Navidad", true, now);
        when(repository.findAll(null, null, true, 0, 10)).thenReturn(List.of(nwd));
        when(repository.count(null, null, true)).thenReturn(1L);

        var result = service.findAll(null, null, true, 0, 10);

        assertEquals(1, result.content().size());
        assertEquals(0, result.page());
        assertEquals(1, result.totalElements());
    }

    @Test
    void patchUpdatesDate() {
        var now = LocalDateTime.now();
        var originalDate = LocalDate.of(2025, 12, 25);
        var newDate = LocalDate.of(2025, 12, 24);
        var nwd = NonWorkingDays.restore(1L, originalDate, "Navidad", true, now);
        when(repository.findById(1L)).thenReturn(Optional.of(nwd));
        when(repository.save(any())).thenAnswer(invocation -> invocation.<NonWorkingDays>getArgument(0));

        var command = new PatchNonWorkingDayCommand(newDate, null, null);
        var result = service.patch(1L, command);

        assertEquals(newDate, result.date());
        assertEquals("Navidad", result.description());
        assertTrue(result.isRecurring());
    }

    @Test
    void patchUpdatesDescription() {
        var now = LocalDateTime.now();
        var date = LocalDate.of(2025, 12, 25);
        var nwd = NonWorkingDays.restore(1L, date, "Navidad", true, now);
        when(repository.findById(1L)).thenReturn(Optional.of(nwd));
        when(repository.save(any())).thenAnswer(invocation -> invocation.<NonWorkingDays>getArgument(0));

        var command = new PatchNonWorkingDayCommand(null, "Nochebuena", null);
        var result = service.patch(1L, command);

        assertEquals("Nochebuena", result.description());
    }

    @Test
    void patchUpdatesRecurring() {
        var now = LocalDateTime.now();
        var date = LocalDate.of(2025, 12, 25);
        var nwd = NonWorkingDays.restore(1L, date, "Navidad", true, now);
        when(repository.findById(1L)).thenReturn(Optional.of(nwd));
        when(repository.save(any())).thenAnswer(invocation -> invocation.<NonWorkingDays>getArgument(0));

        var command = new PatchNonWorkingDayCommand(null, null, false);
        var result = service.patch(1L, command);

        assertFalse(result.isRecurring());
    }

    @Test
    void patchWithNotFoundIdThrowsException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        var command = new PatchNonWorkingDayCommand(LocalDate.now(), null, null);

        assertThrows(IllegalArgumentException.class, () -> service.patch(99L, command));
        verify(repository, never()).save(any());
    }

    @Test
    void deleteByIdDeletesExisting() {
        var now = LocalDateTime.now();
        var nwd = NonWorkingDays.restore(1L, LocalDate.of(2025, 12, 25), "Navidad", true, now);
        when(repository.findById(1L)).thenReturn(Optional.of(nwd));

        service.deleteById(1L);

        verify(repository).deleteById(1L);
    }

    @Test
    void deleteByIdWithNotFoundIdThrowsException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.deleteById(99L));
        verify(repository, never()).deleteById(any());
    }
}
