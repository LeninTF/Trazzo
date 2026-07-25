package trazzo.back.corehr.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.command.CreateTenantUserDepartmentCommand;
import trazzo.back.corehr.application.dto.command.PatchTenantUserDepartmentCommand;
import trazzo.back.corehr.application.port.out.TenantUserDepartmentRepositoryPort;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.corehr.domain.model.employee.TenantUserDepartment;
import trazzo.back.corehr.domain.model.employee.TenantUserDepartment;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Page;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class TenantUserDepartmentServiceTest {

    private TenantUserDepartmentRepositoryPort departmentRepository;
    private TenantUserPort tenantUserPort;
    private TenantUserDepartmentService service;

    @BeforeEach
    void setUp() {
        departmentRepository = mock(TenantUserDepartmentRepositoryPort.class);
        tenantUserPort = mock(TenantUserPort.class);
        service = new TenantUserDepartmentService(departmentRepository, tenantUserPort);
    }

    @Test
    void createWithValidData() {
        when(tenantUserPort.existsById(1L)).thenReturn(true);
        when(departmentRepository.save(any())).thenAnswer(invocation -> {
            var dept = invocation.<TenantUserDepartment>getArgument(0);
            return TenantUserDepartment.restore(1L, dept.getTenantUserId(), dept.getDepartmentId(),
                    dept.isPrimary(), dept.getStartDate(), dept.getEndDate(), dept.getCreatedAt(), dept.getUpdatedAt());
        });

        var command = new CreateTenantUserDepartmentCommand(10L, true, LocalDate.of(2025, 1, 1), null);
        var result = service.create(1L, command);

        assertEquals(1L, result.id());
        assertEquals(1L, result.tenantUserId());
        assertEquals(10L, result.departmentId());
        assertTrue(result.isPrimary());
        assertEquals(LocalDate.of(2025, 1, 1), result.startDate());
        verify(departmentRepository).save(any());
    }

    @Test
    void createWithUserNotFoundThrowsException() {
        when(tenantUserPort.existsById(99L)).thenReturn(false);

        var command = new CreateTenantUserDepartmentCommand(10L, true, LocalDate.of(2025, 1, 1), null);
        assertThrows(IllegalArgumentException.class, () -> service.create(99L, command));
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void createClosesExistingPrimary() {
        var now = LocalDateTime.now();
        var existingPrimary = TenantUserDepartment.restore(5L, 1L, 10L, true, LocalDate.of(2024, 1, 1), null, now, now);
        when(tenantUserPort.existsById(1L)).thenReturn(true);
        when(departmentRepository.findPrimaryByTenantUserId(1L)).thenReturn(Optional.of(existingPrimary));
        when(departmentRepository.save(any())).thenAnswer(invocation -> invocation.<TenantUserDepartment>getArgument(0));

        var command = new CreateTenantUserDepartmentCommand(20L, true, LocalDate.of(2025, 6, 1), null);
        var result = service.create(1L, command);

        assertEquals(LocalDate.of(2025, 5, 31), existingPrimary.getEndDate());
        verify(departmentRepository, times(2)).save(any());
    }

    @Test
    void findAllByTenantUserIdReturnsDepartments() {
        var now = LocalDateTime.now();
        var dept = TenantUserDepartment.restore(1L, 1L, 10L, true, LocalDate.of(2025, 1, 1), null, now, now);
        when(tenantUserPort.existsById(1L)).thenReturn(true);
        when(departmentRepository.findAllByTenantUserId(1L)).thenReturn(List.of(dept));

        var result = service.findAllByTenantUserId(1L);

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).departmentId());
    }

    @Test
    void findAllByTenantUserIdPaginatedReturnsResult() {
        var now = LocalDateTime.now();
        var dept = TenantUserDepartment.restore(1L, 1L, 10L, true, LocalDate.of(2025, 1, 1), null, now, now);
        var page = new PageImpl<>(List.of(dept));
        when(tenantUserPort.existsById(1L)).thenReturn(true);
        when(departmentRepository.findAllByTenantUserId(eq(1L), anyInt(), anyInt())).thenReturn(page);

        var result = service.findAllByTenantUserId(1L, 0, 10);

        assertEquals(1, result.content().size());
        assertEquals(10L, result.content().get(0).departmentId());
    }

    @Test
    void findAllByTenantUserIdWithUserNotFoundThrowsException() {
        when(tenantUserPort.existsById(99L)).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> service.findAllByTenantUserId(99L));
        verify(departmentRepository, never()).findAllByTenantUserId(any());
    }

    @Test
    void findAllByTenantUserIdPaginatedWithUserNotFoundThrowsException() {
        when(tenantUserPort.existsById(99L)).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> service.findAllByTenantUserId(99L, 0, 10));
        verify(departmentRepository, never()).findAllByTenantUserId(anyLong(), anyInt(), anyInt());
    }

    @Test
    void patchUpdatesEndDate() {
        var now = LocalDateTime.now();
        var dept = TenantUserDepartment.restore(1L, 1L, 10L, true, LocalDate.of(2025, 1, 1), null, now, now);
        when(departmentRepository.findByTenantUserIdAndDepartmentId(1L, 10L)).thenReturn(Optional.of(dept));
        when(departmentRepository.save(any())).thenAnswer(invocation -> invocation.<TenantUserDepartment>getArgument(0));

        var command = new PatchTenantUserDepartmentCommand(LocalDate.of(2025, 12, 31), null);
        var result = service.patch(1L, 10L, command);

        assertEquals(LocalDate.of(2025, 12, 31), result.endDate());
        verify(departmentRepository).save(dept);
    }

    @Test
    void patchMarksAsPrimary() {
        var now = LocalDateTime.now();
        var dept = TenantUserDepartment.restore(1L, 1L, 10L, false, LocalDate.of(2025, 1, 1), null, now, now);
        when(departmentRepository.findByTenantUserIdAndDepartmentId(1L, 10L)).thenReturn(Optional.of(dept));
        when(departmentRepository.save(any())).thenAnswer(invocation -> invocation.<TenantUserDepartment>getArgument(0));

        var command = new PatchTenantUserDepartmentCommand(null, true);
        var result = service.patch(1L, 10L, command);

        assertTrue(result.isPrimary());
    }

    @Test
    void patchUnmarksAsPrimary() {
        var now = LocalDateTime.now();
        var dept = TenantUserDepartment.restore(1L, 1L, 10L, true, LocalDate.of(2025, 1, 1), null, now, now);
        when(departmentRepository.findByTenantUserIdAndDepartmentId(1L, 10L)).thenReturn(Optional.of(dept));
        when(departmentRepository.save(any())).thenAnswer(invocation -> invocation.<TenantUserDepartment>getArgument(0));

        var command = new PatchTenantUserDepartmentCommand(null, false);
        var result = service.patch(1L, 10L, command);

        assertFalse(result.isPrimary());
    }

    @Test
    void patchWithNotFoundIdThrowsException() {
        when(departmentRepository.findByTenantUserIdAndDepartmentId(99L, 10L)).thenReturn(Optional.empty());
        var command = new PatchTenantUserDepartmentCommand(LocalDate.of(2025, 12, 31), null);

        assertThrows(IllegalArgumentException.class, () -> service.patch(99L, 10L, command));
        verify(departmentRepository, never()).save(any());
    }
}
