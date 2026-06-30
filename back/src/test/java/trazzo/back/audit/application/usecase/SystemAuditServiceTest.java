package trazzo.back.audit.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.audit.application.dto.result.PaginatedResult;
import trazzo.back.audit.application.dto.result.SystemAuditResult;
import trazzo.back.audit.application.port.out.SystemAuditRepositoryPort;
import trazzo.back.audit.domain.exception.AuditNotFoundException;
import trazzo.back.audit.domain.model.tenant.HttpMethod;
import trazzo.back.audit.domain.model.tenant.SystemActions;
import trazzo.back.audit.domain.model.tenant.SystemAudit;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class SystemAuditServiceTest {

    private SystemAuditRepositoryPort systemAuditRepository;
    private SystemAuditService service;

    @BeforeEach
    void setUp() {
        systemAuditRepository = mock(SystemAuditRepositoryPort.class);
        service = new SystemAuditService(systemAuditRepository);
    }

    @Test
    void findByIdReturnsSystemAudit() {
        var now = LocalDateTime.now();
        var audit = new SystemAudit(1L, "user-1", SystemActions.CREATE, "module", "entity", "entity-1",
                HttpMethod.POST, "/api/test", "desc", Map.of(), Map.of(), "192.168.1.1", "OK", now);
        when(systemAuditRepository.findById(1L)).thenReturn(Optional.of(audit));

        var result = service.findById(1L);

        assertEquals(1L, result.id());
        assertEquals("user-1", result.userTenantId());
        assertEquals(SystemActions.CREATE, result.systemActions());
        assertEquals("module", result.module());
        assertEquals("entity", result.entity());
        assertEquals("entity-1", result.entityId());
        assertEquals(HttpMethod.POST, result.httpMethod());
        assertEquals("/api/test", result.endpoint());
        assertEquals("desc", result.description());
        assertEquals("192.168.1.1", result.ipAddress());
        assertEquals("OK", result.result());
        assertEquals(now, result.createdAt());
    }

    @Test
    void findByIdThrowsWhenNotFound() {
        when(systemAuditRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AuditNotFoundException.class, () -> service.findById(99L));
    }

    @Test
    void findAllReturnsPaginatedResults() {
        var now = LocalDateTime.now();
        var audit = new SystemAudit(1L, "user-1", SystemActions.CREATE, "module", "entity", "entity-1",
                HttpMethod.POST, "/api/test", "desc", Map.of(), Map.of(), "192.168.1.1", "OK", now);
        when(systemAuditRepository.findAll(eq(null), eq(null), eq(null), eq(null), eq(null), any())).thenReturn(List.of(audit));
        when(systemAuditRepository.count(null, null, null, null, null)).thenReturn(1L);

        var result = service.findAll(null, null, null, null, null, 0, 10, null);

        assertEquals(1, result.content().size());
        assertEquals(0, result.page());
        assertEquals(10, result.size());
        assertEquals(1, result.totalElements());
        assertEquals(1, result.totalPages());
        assertEquals("module", result.content().get(0).module());
        verify(systemAuditRepository).findAll(eq(null), eq(null), eq(null), eq(null), eq(null), any());
        verify(systemAuditRepository).count(null, null, null, null, null);
    }

    @Test
    void findAllReturnsEmptyWhenNoAudits() {
        when(systemAuditRepository.findAll(eq(null), eq(null), eq(null), eq(null), eq(null), any())).thenReturn(List.of());
        when(systemAuditRepository.count(null, null, null, null, null)).thenReturn(0L);

        var result = service.findAll(null, null, null, null, null, 0, 10, null);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
    }

}
