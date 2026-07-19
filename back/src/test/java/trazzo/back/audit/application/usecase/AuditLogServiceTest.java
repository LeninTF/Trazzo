package trazzo.back.audit.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
        assertEquals("entity", result.entidad());
        assertEquals("entity-1", result.entidadId());
        assertEquals(Action.CREATE, result.accion());
        assertEquals("user-1", result.userId());
        assertEquals("/api/test", result.endpoint());
        assertEquals("192.168.1.1", result.ipAddress());
        assertEquals("Mozilla/5.0", result.userAgent());
        assertEquals(Map.of(), result.oldValue());
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
        when(auditRepository.findAll(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(List.of(audit));
        when(auditRepository.count(any(), any(), any(), any(), any(), any())).thenReturn(1L);
        when(tenantInfoPort.findByUserIds(any())).thenReturn(Map.of("user-1", new TenantInfoPort.TenantInfo("tenant-1", "Test Corp")));
        when(userInfoPort.findByUserIds(any())).thenReturn(Map.of("user-1", new UserInfoPort.UserInfo("user-1", "John Doe", "john@test.com")));

        var result = service.findAll(null, null, null, null, null, null, 0, 10, null);

        assertEquals(1, result.content().size());
        assertEquals(0, result.page());
        assertEquals(10, result.size());
        assertEquals(1, result.totalElements());
        assertEquals(1, result.totalPages());

        var logResult = result.content().get(0);
        assertEquals("1", logResult.id());
        assertEquals("EVT-1", logResult.eventId());
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

        verify(auditRepository).findAll(any(), any(), any(), any(), any(), any(), any(Pageable.class));
        verify(auditRepository).count(any(), any(), any(), any(), any(), any());
        verify(tenantInfoPort).findByUserIds(any());
        verify(userInfoPort).findByUserIds(any());
    }

    @Test
    void findAllMapsDeleteActionToAdvertencia() {
        var now = LocalDateTime.now();
        var audit = Audit.restore("1", "entity", "entity-1", Action.DELETE, "user-1", "/api/test",
                "192.168.1.1", "Mozilla/5.0", null, null, now);
        when(auditRepository.findAll(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(List.of(audit));
        when(auditRepository.count(any(), any(), any(), any(), any(), any())).thenReturn(1L);
        when(tenantInfoPort.findByUserIds(any())).thenReturn(Map.of());
        when(userInfoPort.findByUserIds(any())).thenReturn(Map.of());

        var result = service.findAll(null, null, null, null, null, null, 0, 10, null);

        assertEquals("DELETE", result.content().get(0).accion());
        assertEquals("advertencia", result.content().get(0).tipo());
    }

    @Test
    void findAllMapsCreateActionToExito() {
        var now = LocalDateTime.now();
        var audit = Audit.restore("1", "entity", "entity-1", Action.CREATE, "user-1", "/api/test",
                "192.168.1.1", "Mozilla/5.0", null, null, now);
        when(auditRepository.findAll(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(List.of(audit));
        when(auditRepository.count(any(), any(), any(), any(), any(), any())).thenReturn(1L);
        when(tenantInfoPort.findByUserIds(any())).thenReturn(Map.of());
        when(userInfoPort.findByUserIds(any())).thenReturn(Map.of());

        var result = service.findAll(null, null, null, null, null, null, 0, 10, null);

        assertEquals("CREATE", result.content().get(0).accion());
        assertEquals("exito", result.content().get(0).tipo());
    }

    @Test
    void findAllHandlesNullUserId() {
        var now = LocalDateTime.now();
        var audit = Audit.restore("1", "entity", "entity-1", Action.UPDATE, null, "/api/test",
                "192.168.1.1", "Mozilla/5.0", null, null, now);
        when(auditRepository.findAll(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(List.of(audit));
        when(auditRepository.count(any(), any(), any(), any(), any(), any())).thenReturn(1L);

        var result = service.findAll(null, null, null, null, null, null, 0, 10, null);

        assertEquals(1, result.content().size());
        assertNull(result.content().get(0).userName());
        assertNull(result.content().get(0).userEmail());
    }

    @Test
    void findAllReturnsEmptyWhenNoAudits() {
        when(auditRepository.findAll(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(List.of());
        when(auditRepository.count(any(), any(), any(), any(), any(), any())).thenReturn(0L);

        var result = service.findAll(null, null, null, null, null, null, 0, 10, null);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
    }

    @Test
    void findAllWithDates_parsesDateStrings() {
        when(auditRepository.findAll(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(List.of());
        when(auditRepository.count(any(), any(), any(), any(), any(), any())).thenReturn(0L);

        var result = service.findAll(null, null, null, null, "2026-01-01", "2026-01-31", 0, 10, null);

        assertTrue(result.content().isEmpty());
        verify(auditRepository).findAll(any(), any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    void findById_enrichesWithUserInfo() {
        var now = LocalDateTime.now();
        var audit = Audit.restore("1", "entity", "entity-1", Action.UPDATE, "user-1", "/api",
                "192.168.1.1", "agent", null, null, now);
        when(auditRepository.findById("1")).thenReturn(Optional.of(audit));

        var result = service.findById("1");

        assertEquals("entity", result.entidad());
        assertEquals(Action.UPDATE, result.accion());
        assertEquals("user-1", result.userId());
        assertEquals("192.168.1.1", result.ipAddress());
        assertEquals("agent", result.userAgent());
    }

    @Test
    void findAll_enrichesWithTenantInfoAndUserInfo() {
        var now = LocalDateTime.now();
        var audit = Audit.restore("2", "entity", "e-2", Action.CREATE, "user-2", "/api",
                "10.0.0.1", "curl", null, null, now);
        when(auditRepository.findAll(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(List.of(audit));
        when(auditRepository.count(any(), any(), any(), any(), any(), any())).thenReturn(1L);
        when(tenantInfoPort.findByUserIds(any())).thenReturn(Map.of("user-2", new TenantInfoPort.TenantInfo("t-2", "Tenant Corp")));
        when(userInfoPort.findByUserIds(any())).thenReturn(Map.of("user-2", new UserInfoPort.UserInfo("user-2", "Jane Smith", "jane@test.com")));

        var result = service.findAll(null, null, null, null, null, null, 0, 10, null);

        var logResult = result.content().get(0);
        assertEquals("Tenant Corp", logResult.tenant());
        assertEquals("t-2", logResult.tenantId());
        assertEquals("Jane Smith", logResult.userName());
        assertEquals("jane@test.com", logResult.userEmail());
    }

    @Test
    void findAll_handlesMissingUserAndTenantInfo() {
        var now = LocalDateTime.now();
        var audit = Audit.restore("3", "entity", "e-3", Action.DELETE, "user-3", "/api",
                "10.0.0.1", "curl", null, null, now);
        when(auditRepository.findAll(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(List.of(audit));
        when(auditRepository.count(any(), any(), any(), any(), any(), any())).thenReturn(1L);
        when(tenantInfoPort.findByUserIds(any())).thenReturn(Map.of());
        when(userInfoPort.findByUserIds(any())).thenReturn(Map.of());

        var result = service.findAll(null, null, null, null, null, null, 0, 10, null);

        var logResult = result.content().get(0);
        assertNull(logResult.tenant());
        assertNull(logResult.tenantId());
        assertNull(logResult.userName());
        assertNull(logResult.userEmail());
    }

    @Test
    void findAll_totalPagesCalculatedCorrectly() {
        when(auditRepository.findAll(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(List.of());
        when(auditRepository.count(any(), any(), any(), any(), any(), any())).thenReturn(25L);

        var result = service.findAll(null, null, null, null, null, null, 0, 10, null);

        assertEquals(3, result.totalPages());
        assertEquals(25, result.totalElements());
    }

    @Test
    void toDetailResult_mapsAllFields() {
        var now = LocalDateTime.now();
        var audit = Audit.restore("id-1", "User", "user-123", Action.UPDATE,
                "u-1", "/api/users", "127.0.0.1", "Mozilla",
                Map.of("name", "old"), Map.of("name", "new"), now);
        when(auditRepository.findById("id-1")).thenReturn(Optional.of(audit));

        var result = service.findById("id-1");

        assertEquals("id-1", result.id());
        assertEquals("User", result.entidad());
        assertEquals("user-123", result.entidadId());
        assertEquals(Action.UPDATE, result.accion());
        assertEquals("u-1", result.userId());
        assertEquals("/api/users", result.endpoint());
        assertEquals("127.0.0.1", result.ipAddress());
        assertEquals("Mozilla", result.userAgent());
        assertEquals(Map.of("name", "old"), result.oldValue());
        assertEquals(Map.of("name", "new"), result.newValue());
        assertEquals(now, result.createdAt());
    }
}
