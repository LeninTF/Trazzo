package trazzo.back.audit.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.audit.application.dto.result.LogInHistoryResult;
import trazzo.back.audit.application.dto.result.PaginatedResult;
import trazzo.back.audit.application.port.out.LogInHistoryRepositoryPort;
import trazzo.back.audit.domain.exception.AuditNotFoundException;
import trazzo.back.audit.domain.model.master.LogInHistory;
import trazzo.back.audit.domain.model.master.StatusLogin;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class LoginHistoryServiceTest {

    private LogInHistoryRepositoryPort loginHistoryRepository;
    private LoginHistoryService service;

    @BeforeEach
    void setUp() {
        loginHistoryRepository = mock(LogInHistoryRepositoryPort.class);
        service = new LoginHistoryService(loginHistoryRepository);
    }

    @Test
    void findByIdReturnsLogInHistory() {
        var now = LocalDateTime.now();
        var log = new LogInHistory("1", "user-1", "email@test.com", StatusLogin.SUCCESS,
                "192.168.1.1", "Mozilla/5.0", now);
        when(loginHistoryRepository.findById("1")).thenReturn(Optional.of(log));

        var result = service.findById("1");

        assertEquals("1", result.id());
        assertEquals("user-1", result.userId());
        assertEquals("email@test.com", result.attemptedEmail());
        assertEquals(StatusLogin.SUCCESS, result.status());
        assertEquals("192.168.1.1", result.ipAddress());
        assertEquals("Mozilla/5.0", result.userAgent());
        assertEquals(now, result.createdAt());
    }

    @Test
    void findByIdThrowsWhenNotFound() {
        when(loginHistoryRepository.findById("99")).thenReturn(Optional.empty());

        assertThrows(AuditNotFoundException.class, () -> service.findById("99"));
    }

    @Test
    void findAllReturnsPaginatedResults() {
        var now = LocalDateTime.now();
        var log = new LogInHistory("1", "user-1", "email@test.com", StatusLogin.SUCCESS,
                "192.168.1.1", "Mozilla/5.0", now);
        when(loginHistoryRepository.findAll(eq(null), eq(null), eq(null), eq(null), eq(null), any())).thenReturn(List.of(log));
        when(loginHistoryRepository.count(null, null, null, null, null)).thenReturn(1L);

        var result = service.findAll(null, null, null, null, null, 0, 10, null);

        assertEquals(1, result.content().size());
        assertEquals(0, result.page());
        assertEquals(10, result.size());
        assertEquals(1, result.totalElements());
        assertEquals(1, result.totalPages());
        assertEquals("user-1", result.content().get(0).userId());
        verify(loginHistoryRepository).findAll(eq(null), eq(null), eq(null), eq(null), eq(null), any());
        verify(loginHistoryRepository).count(null, null, null, null, null);
    }

    @Test
    void findAllReturnsEmptyWhenNoLogs() {
        when(loginHistoryRepository.findAll(eq(null), eq(null), eq(null), eq(null), eq(null), any())).thenReturn(List.of());
        when(loginHistoryRepository.count(null, null, null, null, null)).thenReturn(0L);

        var result = service.findAll(null, null, null, null, null, 0, 10, null);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
    }

}
