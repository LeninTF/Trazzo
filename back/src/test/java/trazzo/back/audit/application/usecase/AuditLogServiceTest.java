package trazzo.back.audit.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
<<<<<<< Updated upstream
import static org.mockito.ArgumentMatchers.any;
=======
>>>>>>> Stashed changes
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.audit.application.dto.result.AuditLogDetailResult;
import trazzo.back.audit.application.dto.result.AuditLogResult;
import trazzo.back.audit.application.dto.result.PaginatedResult;
import trazzo.back.audit.application.port.out.AuditRepositoryPort;
import trazzo.back.audit.application.port.out.TenantInfoPort;
import trazzo.back.audit.application.port.out.UserInfoPort;
import trazzo.back.audit.domain.exception.AuditNotFoundException;
import trazzo.back.audit.domain.model.master.Action;
import trazzo.back.audit.domain.model.master.Audit;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class AuditLogServiceTest {

    private AuditRepositoryPort auditRepository;
    private UserInfoPort userInfoPort;
    private TenantInfoPort tenantInfoPort;
    private AuditLogService service;

    @BeforeEach
    void setUp() {
        auditRepository = mock(AuditRepositoryPort.class);
        userInfoPort = mock(UserInfoPort.class);
        tenantInfoPort = mock(TenantInfoPort.class);
        service = new AuditLogService(auditRepository, userInfoPort, tenantInfoPort);
    }

    @Test
    void findByIdReturnsAuditLogDetail() {
        var now = LocalDateTime.now();
        var audit = Audit.restore("1", "entity", "entity-1", Action.CREATE, "user-1", "/api/test",
                "192.168.1.1", "Mozilla/5.0", Map.of(), Map.of(), now);
        when(auditRepository.findById("1")).thenReturn(Optional.of(audit));

        var result = service.findById("1");

        assertEquals("1", result.id());
        assertEquals("entity", result.entity());
        assertEquals("entity-1", result.entityId());
        assertEquals(Action.CREATE, result.action());
        assertEquals("user-1", result.userId());
        assertEquals("/api/test", result.endpoint());
        assertEquals("192.168.1.1", result.ipAdress());
        assertEquals("Mozilla/5.0", result.userAgent());
        assertEquals(Map.of(), result.previousValue());
        assertEquals(Map.of(), result.newValue());
        assertEquals(now, result.createdAt());
    }

    @Test
    void findByIdThrowsWhenNotFound() {
        when(auditRepository.findById("99")).thenReturn(Optional.empty());

        assertThrows(AuditNotFoundException.class, () -> service.findById("99"));
    }

    @Test
    void findAllReturnsPaginatedResults() {
        var now = LocalDateTime.now();
        var audit = Audit.restore("1", "entity", "entity-1", Action.CREATE, "user-1", "/api/test",
                "192.168.1.1", "Mozilla/5.0", Map.of("old", "val"), Map.of("new", "val"), now);
<<<<<<< Updated upstream
        when(auditRepository.findAll(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(List.of(audit));
        when(auditRepository.count(any(), any(), any(), any(), any(), any())).thenReturn(1L);
        when(tenantInfoPort.findByUserId("user-1")).thenReturn(Optional.of(new TenantInfoPort.TenantInfo("tenant-1", "Test Corp")));
=======
        when(auditRepository.findAll(eq(null), eq(null), eq(null), eq(null), eq(null), any())).thenReturn(List.of(audit));
        when(auditRepository.count(null, null, null, null, null)).thenReturn(1L);
        when(tenantInfoPort.findByTenantId("user-1")).thenReturn(Optional.of(new TenantInfoPort.TenantInfo("tenant-1", "Test Corp")));
>>>>>>> Stashed changes
        when(userInfoPort.findByUserId("user-1")).thenReturn(Optional.of(new UserInfoPort.UserInfo("user-1", "John Doe", "john@test.com")));

        var result = service.findAll(null, null, null, null, null, null, 0, 10, null);

        assertEquals(1, result.content().size());
        assertEquals(0, result.page());
        assertEquals(10, result.size());
        assertEquals(1, result.totalElements());
        assertEquals(1, result.totalPages());

        var logResult = result.content().get(0);
        assertEquals("1", logResult.id());
        assertEquals("EVT-1-X", logResult.eventId());
        assertEquals(now, logResult.fecha());
        assertEquals("Test Corp", logResult.tenant());
        assertEquals("tenant-1", logResult.tenantId());
        assertEquals("John Doe", logResult.userName());
        assertEquals("john@test.com", logResult.userEmail());
        assertEquals("CREATE", logResult.accion());
        assertEquals("exito", logResult.tipo());
        assertEquals("entity", logResult.entidad());
        assertEquals("entity-1", logResult.entidadId());
        assertEquals("192.168.1.1", logResult.ipAddress());
        assertEquals("Mozilla/5.0", logResult.userAgent());

<<<<<<< Updated upstream
        verify(auditRepository).findAll(any(), any(), any(), any(), any(), any(), any(Pageable.class));
        verify(auditRepository).count(any(), any(), any(), any(), any(), any());
        verify(tenantInfoPort).findByUserId("user-1");
=======
        verify(auditRepository).findAll(eq(null), eq(null), eq(null), eq(null), eq(null), any());
        verify(auditRepository).count(null, null, null, null, null);
        verify(tenantInfoPort).findByTenantId("user-1");
>>>>>>> Stashed changes
        verify(userInfoPort).findByUserId("user-1");
    }

    @Test
    void findAllMapsDeleteActionToAdvertencia() {
        var now = LocalDateTime.now();
        var audit = Audit.restore("1", "entity", "entity-1", Action.DELETE, "user-1", "/api/test",
                "192.168.1.1", "Mozilla/5.0", null, null, now);
<<<<<<< Updated upstream
        when(auditRepository.findAll(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(List.of(audit));
        when(auditRepository.count(any(), any(), any(), any(), any(), any())).thenReturn(1L);
        when(tenantInfoPort.findByUserId("user-1")).thenReturn(Optional.empty());
=======
        when(auditRepository.findAll(eq(null), eq(null), eq(null), eq(null), eq(null), any())).thenReturn(List.of(audit));
        when(auditRepository.count(null, null, null, null, null)).thenReturn(1L);
        when(tenantInfoPort.findByTenantId("user-1")).thenReturn(Optional.empty());
>>>>>>> Stashed changes
        when(userInfoPort.findByUserId("user-1")).thenReturn(Optional.empty());

        var result = service.findAll(null, null, null, null, null, null, 0, 10, null);

        assertEquals("DELETE", result.content().get(0).accion());
        assertEquals("advertencia", result.content().get(0).tipo());
    }

    @Test
    void findAllMapsCreateActionToExito() {
        var now = LocalDateTime.now();
        var audit = Audit.restore("1", "entity", "entity-1", Action.CREATE, "user-1", "/api/test",
                "192.168.1.1", "Mozilla/5.0", null, null, now);
<<<<<<< Updated upstream
        when(auditRepository.findAll(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(List.of(audit));
        when(auditRepository.count(any(), any(), any(), any(), any(), any())).thenReturn(1L);
        when(tenantInfoPort.findByUserId("user-1")).thenReturn(Optional.empty());
=======
        when(auditRepository.findAll(eq(null), eq(null), eq(null), eq(null), eq(null), any())).thenReturn(List.of(audit));
        when(auditRepository.count(null, null, null, null, null)).thenReturn(1L);
        when(tenantInfoPort.findByTenantId("user-1")).thenReturn(Optional.empty());
>>>>>>> Stashed changes
        when(userInfoPort.findByUserId("user-1")).thenReturn(Optional.empty());

        var result = service.findAll(null, null, null, null, null, null, 0, 10, null);

        assertEquals("CREATE", result.content().get(0).accion());
        assertEquals("exito", result.content().get(0).tipo());
    }

    @Test
    void findAllHandlesNullUserId() {
        var now = LocalDateTime.now();
        var audit = Audit.restore("1", "entity", "entity-1", Action.UPDATE, null, "/api/test",
                "192.168.1.1", "Mozilla/5.0", null, null, now);
<<<<<<< Updated upstream
        when(auditRepository.findAll(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(List.of(audit));
        when(auditRepository.count(any(), any(), any(), any(), any(), any())).thenReturn(1L);
=======
        when(auditRepository.findAll(eq(null), eq(null), eq(null), eq(null), eq(null), any())).thenReturn(List.of(audit));
        when(auditRepository.count(null, null, null, null, null)).thenReturn(1L);
>>>>>>> Stashed changes

        var result = service.findAll(null, null, null, null, null, null, 0, 10, null);

        assertEquals(1, result.content().size());
        assertNull(result.content().get(0).userName());
        assertNull(result.content().get(0).userEmail());
<<<<<<< Updated upstream
        verify(tenantInfoPort, never()).findByUserId(any());
=======
        // tenantInfo is still looked up with null userId
        verify(tenantInfoPort).findByTenantId(null);
>>>>>>> Stashed changes
        verify(userInfoPort, never()).findByUserId(any());
    }

    @Test
    void findAllReturnsEmptyWhenNoAudits() {
<<<<<<< Updated upstream
        when(auditRepository.findAll(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(List.of());
        when(auditRepository.count(any(), any(), any(), any(), any(), any())).thenReturn(0L);
=======
        when(auditRepository.findAll(eq(null), eq(null), eq(null), eq(null), eq(null), any())).thenReturn(List.of());
        when(auditRepository.count(null, null, null, null, null)).thenReturn(0L);
>>>>>>> Stashed changes

        var result = service.findAll(null, null, null, null, null, null, 0, 10, null);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
    }

}
