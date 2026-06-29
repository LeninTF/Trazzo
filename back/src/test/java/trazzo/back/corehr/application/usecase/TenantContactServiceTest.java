package trazzo.back.corehr.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.corehr.application.dto.command.CreateTenantContactCommand;
import trazzo.back.corehr.application.dto.command.PatchTenantContactCommand;
import trazzo.back.corehr.application.dto.result.PaginatedResult;
import trazzo.back.corehr.application.port.out.TenantContactRepositoryPort;
import trazzo.back.corehr.application.port.out.TenantUserPort;
import trazzo.back.corehr.domain.model.employee.TenantContact;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class TenantContactServiceTest {

    private TenantContactRepositoryPort repository;
    private TenantUserPort tenantUserPort;
    private TenantContactService service;

    @BeforeEach
    void setUp() {
        repository = mock(TenantContactRepositoryPort.class);
        tenantUserPort = mock(TenantUserPort.class);
        service = new TenantContactService(repository, tenantUserPort);
    }

    @Test
    void createWithValidData() {
        when(tenantUserPort.existsById("1")).thenReturn(true);
        when(repository.save(any())).thenAnswer(invocation -> {
            var contact = invocation.<TenantContact>getArgument(0);
            return TenantContact.restore(1L, contact.getTenantUserId(), contact.getType(), contact.getCreatedAt(), contact.getUpdatedAt(), null);
        });

        var command = new CreateTenantContactCommand(1L, "EMAIL");
        var result = service.create(command);

        assertEquals(1L, result.id());
        assertEquals(1L, result.tenantUserId());
        assertEquals("EMAIL", result.type());
        verify(repository).save(any());
    }

    @Test
    void createWithUserNotFoundThrowsException() {
        when(tenantUserPort.existsById("99")).thenReturn(false);

        var command = new CreateTenantContactCommand(99L, "EMAIL");
        assertThrows(IllegalArgumentException.class, () -> service.create(command));
        verify(repository, never()).save(any());
    }

    @Test
    void findAllReturnsPaginatedResults() {
        var now = LocalDateTime.now();
        var contact = TenantContact.restore(1L, 1L, "EMAIL", now, now, null);
        when(repository.findAll(0, 10)).thenReturn(List.of(contact));
        when(repository.count()).thenReturn(1L);

        PaginatedResult<?> result = service.findAll(0, 10);

        assertEquals(1, result.content().size());
        assertEquals(0, result.page());
        assertEquals(1, result.totalElements());
    }

    @Test
    void patchUpdatesType() {
        var now = LocalDateTime.now();
        var contact = TenantContact.restore(1L, 1L, "EMAIL", now, now, now);
        when(repository.findById(1L)).thenReturn(Optional.of(contact));
        when(repository.save(any())).thenAnswer(invocation -> invocation.<TenantContact>getArgument(0));

        var command = new PatchTenantContactCommand("PHONE");
        var result = service.patch(1L, command);

        assertEquals("PHONE", result.type());
        verify(repository).save(contact);
    }

    @Test
    void patchWithNotFoundIdThrowsException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        var command = new PatchTenantContactCommand("PHONE");

        assertThrows(IllegalArgumentException.class, () -> service.patch(99L, command));
        verify(repository, never()).save(any());
    }

    @Test
    void deleteByIdMarksAsDeleted() {
        var now = LocalDateTime.now();
        var contact = TenantContact.restore(1L, 1L, "EMAIL", now, now, null);
        when(repository.findById(1L)).thenReturn(Optional.of(contact));
        when(repository.save(any())).thenAnswer(invocation -> invocation.<TenantContact>getArgument(0));

        service.deleteById(1L);

        assertNotNull(contact.getDeletedAt());
        verify(repository).save(contact);
    }

    @Test
    void deleteByIdWithNotFoundIdThrowsException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.deleteById(99L));
        verify(repository, never()).save(any());
    }
}
