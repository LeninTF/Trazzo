package trazzo.back.reports.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import trazzo.back.reports.application.dto.command.ListMonthlyClosuresCommand;
import trazzo.back.reports.application.dto.result.MonthlyClosureResult;
import trazzo.back.reports.application.ports.out.MonthlyClosureRepositoryPort;
import trazzo.back.reports.domain.model.closure.MonthlyClosure;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ListMonthlyClosureServiceTest {

    @Mock
    private MonthlyClosureRepositoryPort closureRepository;

    private ListMonthlyClosureService service;

    @BeforeEach
    void setUp() {
        service = new ListMonthlyClosureService(closureRepository);
    }

    @Test
    void shouldReturnAllClosuresWhenNoFilter() {
        MonthlyClosure c1 = new MonthlyClosure(UUID.randomUUID(), 6, 2025, 10, "e1", "p1", "u1", LocalDateTime.now());
        MonthlyClosure c2 = new MonthlyClosure(UUID.randomUUID(), 7, 2025, 5, "e2", "p2", "u2", LocalDateTime.now());
        when(closureRepository.findAll()).thenReturn(List.of(c1, c2));

        List<MonthlyClosureResult> results = service.execute(new ListMonthlyClosuresCommand(null, null));

        assertEquals(2, results.size());
    }

    @Test
    void shouldReturnEmptyListWhenNoClosures() {
        when(closureRepository.findAll()).thenReturn(List.of());

        List<MonthlyClosureResult> results = service.execute(new ListMonthlyClosuresCommand(null, null));

        assertTrue(results.isEmpty());
    }

    @Test
    void shouldMapAllFieldsCorrectly() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosure closure = new MonthlyClosure(id, 6, 2025, 10, "excel", "pdf", "user-1", now);
        when(closureRepository.findAll()).thenReturn(List.of(closure));

        List<MonthlyClosureResult> results = service.execute(new ListMonthlyClosuresCommand(null, null));
        MonthlyClosureResult result = results.getFirst();

        assertEquals(id, result.id());
        assertEquals(6, result.month());
        assertEquals(2025, result.year());
        assertEquals(10, result.totalEmployees());
        assertEquals("excel", result.excelReportUrl());
        assertEquals("pdf", result.pdfReportUrl());
        assertEquals(now, result.createdAt());
    }

    @Test
    void shouldFilterByMonthAndYear() {
        MonthlyClosure closure = new MonthlyClosure(UUID.randomUUID(), 6, 2025, 10, "e", "p", "u", LocalDateTime.now());
        when(closureRepository.findByMonthAndYear(6, 2025)).thenReturn(List.of(closure));

        List<MonthlyClosureResult> results = service.execute(new ListMonthlyClosuresCommand(2025, 6));

        assertEquals(1, results.size());
        verify(closureRepository).findByMonthAndYear(6, 2025);
        verify(closureRepository, never()).findAll();
    }
}
