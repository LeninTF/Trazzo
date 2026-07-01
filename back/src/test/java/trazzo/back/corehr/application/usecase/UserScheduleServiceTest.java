package trazzo.back.corehr.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.command.CreateUserScheduleCommand;
import trazzo.back.corehr.application.dto.result.ScheduleResult;
import trazzo.back.corehr.application.port.out.ScheduleRepositoryPort;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.corehr.application.port.out.UserScheduleRepositoryPort;
import trazzo.back.corehr.domain.model.TenantUserState;
import trazzo.back.corehr.domain.model.schedule.Schedule;
import trazzo.back.corehr.domain.model.schedule.UserSchedule;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

class UserScheduleServiceTest {

    private UserScheduleRepositoryPort userScheduleRepository;
    private ScheduleRepositoryPort scheduleRepository;
    private TenantUserPort tenantUserPort;
    private UserScheduleService service;

    @BeforeEach
    void setUp() {
        userScheduleRepository = mock(UserScheduleRepositoryPort.class);
        scheduleRepository = mock(ScheduleRepositoryPort.class);
        tenantUserPort = mock(TenantUserPort.class);
        service = new UserScheduleService(userScheduleRepository, scheduleRepository, tenantUserPort);
    }

    @Test
    void createWithValidData() {
        var now = LocalDateTime.now();
        var schedule = Schedule.restore(1L, 1L, "Horario A", "Desc",
                LocalTime.of(8, 0), LocalTime.of(17, 0), now, now);
        when(tenantUserPort.existsById(10L)).thenReturn(true);
        when(tenantUserPort.findStateById(10L)).thenReturn(Optional.of(TenantUserState.ACTIVO));
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(userScheduleRepository.save(any())).thenAnswer(invocation -> invocation.<UserSchedule>getArgument(0));

        var command = new CreateUserScheduleCommand(10L, 1L, "Desc", LocalTime.of(8, 0), LocalTime.of(17, 0));
        var result = service.create(command);

        assertEquals(10L, result.tenantUserId());
        assertEquals(1L, result.scheduleId());
        verify(userScheduleRepository).save(any());
    }

    @Test
    void createWithTenantUserNotFoundThrowsException() {
        when(tenantUserPort.existsById(99L)).thenReturn(false);

        var command = new CreateUserScheduleCommand(99L, 1L, "Desc", LocalTime.of(8, 0), LocalTime.of(17, 0));

        assertThrows(IllegalArgumentException.class, () -> service.create(command));
        verify(userScheduleRepository, never()).save(any());
    }

    @Test
    void createWithInactiveUserThrowsException() {
        when(tenantUserPort.existsById(10L)).thenReturn(true);
        when(tenantUserPort.findStateById(10L)).thenReturn(Optional.of(TenantUserState.INACTIVO));

        var command = new CreateUserScheduleCommand(10L, 1L, "Desc", LocalTime.of(8, 0), LocalTime.of(17, 0));

        assertThrows(IllegalStateException.class, () -> service.create(command));
        verify(userScheduleRepository, never()).save(any());
    }

    @Test
    void createWithScheduleNotFoundThrowsException() {
        when(tenantUserPort.existsById(10L)).thenReturn(true);
        when(tenantUserPort.findStateById(10L)).thenReturn(Optional.of(TenantUserState.ACTIVO));
        when(scheduleRepository.findById(99L)).thenReturn(Optional.empty());

        var command = new CreateUserScheduleCommand(10L, 99L, "Desc", LocalTime.of(8, 0), LocalTime.of(17, 0));

        assertThrows(IllegalArgumentException.class, () -> service.create(command));
        verify(userScheduleRepository, never()).save(any());
    }

    @Test
    void findAllReturnsPaginatedResults() {
        var now = LocalDateTime.now();
        var us = UserSchedule.restore(1L, 10L, 1L, "Desc",
                LocalTime.of(8, 0), LocalTime.of(17, 0), now, now);
        var schedule = Schedule.restore(1L, 1L, "Horario A", "Desc",
                LocalTime.of(8, 0), LocalTime.of(17, 0), now, now);
        when(userScheduleRepository.findAll(10L, 1L, 0, 10)).thenReturn(List.of(us));
        when(userScheduleRepository.count(10L, 1L)).thenReturn(1L);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        var result = service.findAll(10L, 1L, 0, 10);

        assertEquals(1, result.content().size());
        assertEquals(0, result.page());
        assertEquals(1, result.totalElements());
    }

    @Test
    void deleteByIdSuccess() {
        var now = LocalDateTime.now();
        var us = UserSchedule.restore(1L, 10L, 1L, "Desc",
                LocalTime.of(8, 0), LocalTime.of(17, 0), now, now);
        when(userScheduleRepository.findById(1L)).thenReturn(Optional.of(us));

        service.deleteById(1L);

        verify(userScheduleRepository).deleteById(1L);
    }

    @Test
    void deleteByIdThrowsExceptionWhenNotFound() {
        when(userScheduleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.deleteById(99L));
        verify(userScheduleRepository, never()).deleteById(any());
    }
}
