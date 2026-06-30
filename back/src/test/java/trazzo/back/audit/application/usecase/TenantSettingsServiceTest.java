package trazzo.back.audit.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import trazzo.back.audit.application.dto.result.PaginatedResult;
import trazzo.back.audit.application.dto.result.TenantSettingsRecordResult;
import trazzo.back.audit.application.port.out.TenantSettingsRecordRepositoryPort;
import trazzo.back.audit.domain.exception.AuditNotFoundException;
import trazzo.back.audit.domain.model.master.TenantSettingsRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

class TenantSettingsServiceTest {

    private TenantSettingsRecordRepositoryPort settingsRepository;
    private TenantSettingsService service;

    @BeforeEach
    void setUp() {
        settingsRepository = mock(TenantSettingsRecordRepositoryPort.class);
        service = new TenantSettingsService(settingsRepository);
    }

    @Test
    void findByIdReturnsTenantSettingsRecord() {
        var now = LocalDateTime.now();
        var record = new TenantSettingsRecord(1L, "ts-1", "dbName", "dbHost", "dbUser", "dbPass", "user-1", "reason", now);
        when(settingsRepository.findById(1L)).thenReturn(Optional.of(record));

        var result = service.findById(1L);

        assertEquals(1L, result.id());
        assertEquals("ts-1", result.tenantSettingId());
        assertEquals("dbName", result.dbName());
        assertEquals("dbHost", result.dbHost());
        assertEquals("dbUser", result.dbUser());
        assertEquals("user-1", result.userId());
        assertEquals("reason", result.changeReason());
        assertEquals(now, result.createdAt());
    }

    @Test
    void findByIdThrowsWhenNotFound() {
        when(settingsRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AuditNotFoundException.class, () -> service.findById(99L));
    }

    @Test
    void findAllReturnsPaginatedResults() {
        var now = LocalDateTime.now();
        var record = new TenantSettingsRecord(1L, "ts-1", "dbName", "dbHost", "dbUser", "dbPass", "user-1", "reason", now);
        when(settingsRepository.findAll(eq(null), eq(null), eq(null), eq(null), eq(null), any())).thenReturn(List.of(record));
        when(settingsRepository.count(null, null, null, null, null)).thenReturn(1L);

        var result = service.findAll(null, null, null, null, null, 0, 10, null);

        assertEquals(1, result.content().size());
        assertEquals(0, result.page());
        assertEquals(10, result.size());
        assertEquals(1, result.totalElements());
        assertEquals(1, result.totalPages());
        assertEquals("ts-1", result.content().get(0).tenantSettingId());
        verify(settingsRepository).findAll(eq(null), eq(null), eq(null), eq(null), eq(null), any());
        verify(settingsRepository).count(null, null, null, null, null);
    }

    @Test
    void findAllReturnsEmptyWhenNoRecords() {
        when(settingsRepository.findAll(eq(null), eq(null), eq(null), eq(null), eq(null), any())).thenReturn(List.of());
        when(settingsRepository.count(null, null, null, null, null)).thenReturn(0L);

        var result = service.findAll(null, null, null, null, null, 0, 10, null);

        assertTrue(result.content().isEmpty());
        assertEquals(0, result.totalElements());
        assertEquals(0, result.totalPages());
    }

}
