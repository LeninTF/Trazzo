package trazzo.back.reports.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import trazzo.back.reports.application.dto.command.GetMonthlyClosureCommand;
import trazzo.back.reports.application.dto.result.MonthlyClosureResult;
import trazzo.back.reports.application.ports.out.MonthlyClosureRepositoryPort;
import trazzo.back.reports.domain.exception.MonthlyClosureNotFoundException;
import trazzo.back.reports.domain.model.closure.MonthlyClosure;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class GetMonthlyClosureServiceTest {

    @Mock
    private MonthlyClosureRepositoryPort closureRepository;

    private GetMonthlyClosureService service;

    @BeforeEach
    void setUp() {
        service = new GetMonthlyClosureService(closureRepository);
    }

    @Test
    void shouldReturnClosureWhenFound() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosure closure = new MonthlyClosure(
                id, 6, 2025, 10, "excel", "pdf", UUID.randomUUID(), now);
        when(closureRepository.findById(id)).thenReturn(Optional.of(closure));

        MonthlyClosureResult result = service.execute(new GetMonthlyClosureCommand(id));

        assertEquals(id, result.id());
        assertEquals(6, result.month());
        assertEquals(2025, result.year());
        assertEquals(10, result.totalEmployees());
        assertEquals("excel", result.excelReportUrl());
        assertEquals("pdf", result.pdfReportUrl());
        assertEquals(now, result.createdAt());
    }

    @Test
    void shouldThrowExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(closureRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(MonthlyClosureNotFoundException.class, () -> service.execute(new GetMonthlyClosureCommand(id)));
    }
}
