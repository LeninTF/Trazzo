package trazzo.back.audit.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.audit.application.dto.result.PaginatedResult;
import trazzo.back.audit.application.dto.result.SessionResult;
import trazzo.back.audit.application.port.out.SessionRepositoryPort;
import trazzo.back.audit.domain.exception.AuditNotFoundException;
import trazzo.back.audit.domain.model.tenant.Session;
import trazzo.back.audit.domain.model.tenant.SessionState;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class SessionServiceTest {

    private SessionRepositoryPort sessionRepository;
    private SessionService service;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(SessionRepositoryPort.class);
        service = new SessionService(sessionRepository);
    }

    @Test
    void findByIdReturnsSession() {
        var now = LocalDateTime.now();
        var session = new Session(1L, "user-1", "hash-123", "192.168.1.1",
                "Mozilla/5.0", "fp-123", now, now, null, now.plusHours(1),
                SessionState.ACTIVE, now, now);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));

        var result = service.findById(1L);

        assertEquals(1L, result.id());
        assertEquals("user-1", result.tenantUserId());
        assertEquals("hash-123", result.refreshTokenHash());
        assertEquals("192.168.1.1", result.ipAddress());
        assertEquals("Mozilla/5.0", result.userAgent());
        assertEquals("fp-123", result.deviceFingerprint());
        assertEquals(now, result.loginAt());
        assertEquals(now, result.lasActivityAt());
        assertNull(result.logoutAt());
        assertEquals(now.plusHours(1), result.expiresAt());
        assertEquals(SessionState.ACTIVE, result.state());
        assertEquals(now, result.createdAt());
        assertEquals(now, result.updatedAt());
    }

    @Test
    void findByIdThrowsWhenNotFound() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AuditNotFoundException.class, () -> service.findById(99L));
    }

    @Test
    void findAllReturnsPaginatedResults() {
        var now = LocalDateTime.now();
        var session = new Session(1L, "user-1", "hash-123", "192.168.1.1",
                "Mozilla/5.0", "fp-123", now, now, null, now.plusHours(1),
                SessionState.ACTIVE, now, now);
        when(sessionRepository.findAll(eq(null), eq(null), eq(null), any())).thenReturn(List.of(session));
        when(sessionRepository.count(null, null, null)).thenReturn(1L);

        var result = service.findAll(null, null, null, 0, 10, null);

        assertEquals(1, result.content().size());
        assertEquals(0, result.page());
        assertEquals(10, result.size());
        assertEquals(1, result.totalElements());
        assertEquals(1, result.totalPages());
        assertEquals("user-1", result.content().get(0).tenantUserId());
        verify(sessionRepository).findAll(eq(null), eq(null), eq(null), any());
        verify(sessionRepository).count(null, null, null);
    }

    @Test
    void findAllReturnsEmptyWhenNoSessions() {
        when(sessionRepository.findAll(eq(null), eq(null), eq(null), any())).thenReturn(List.of());
        when(sessionRepository.count(null, null, null)).thenReturn(0L);

        var result = service.findAll(null, null, null, 0, 10, null);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
    }

}
