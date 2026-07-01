package trazzo.back.reports.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import trazzo.back.reports.application.dto.result.MonthlyClosureWithDetailsResult;
import trazzo.back.reports.application.ports.out.MonthlyClosureDetailRepositoryPort;
import trazzo.back.reports.application.ports.out.MonthlyClosureRepositoryPort;
import trazzo.back.reports.domain.exception.MonthlyClosureNotFoundException;
import trazzo.back.reports.domain.model.closure.MonthlyClosure;
import trazzo.back.reports.domain.model.closure.MonthlyClosureDetail;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class GetMonthlyReportServiceTest {

    @Mock
    private MonthlyClosureRepositoryPort closureRepository;
    @Mock
    private MonthlyClosureDetailRepositoryPort detailRepository;

    private GetMonthlyReportService service;

    @BeforeEach
    void setUp() {
        service = new GetMonthlyReportService(closureRepository, detailRepository);
    }

    @Test
    void shouldReturnClosureWithDetails() {
        UUID closureId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosure closure = new MonthlyClosure(closureId, 6, 2025, 2, "excel", "pdf", UUID.randomUUID(), now);

        MonthlyClosureDetail detail1 = new MonthlyClosureDetail(
                UUID.randomUUID(), closureId, 1L, "Juan", "111", "TI", "Dev",
                160.0, 10, 1, 5.0, now);
        MonthlyClosureDetail detail2 = new MonthlyClosureDetail(
                UUID.randomUUID(), closureId, 2L, "Ana", "222", "HR", "Mgr",
                80.0, 15, 2, 0.0, now);

        when(closureRepository.findById(closureId)).thenReturn(Optional.of(closure));
        when(detailRepository.findByMonthlyClosureId(closureId)).thenReturn(List.of(detail1, detail2));

        MonthlyClosureWithDetailsResult result = service.execute(closureId);

        assertEquals(closureId, result.id());
        assertEquals(6, result.month());
        assertEquals(2025, result.year());
        assertEquals(2, result.totalEmployees());
        assertEquals(now, result.createdAt());
        assertEquals(2, result.details().size());
        assertEquals(closureId, result.details().getFirst().monthClosureId());
        assertEquals(closureId, result.details().get(1).monthClosureId());
        assertEquals(now, result.details().getFirst().createdAt());
        assertEquals(now, result.details().get(1).createdAt());
    }

    @Test
    void shouldReturnClosureWithEmptyDetails() {
        UUID closureId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosure closure = new MonthlyClosure(closureId, 6, 2025, 0, null, null, UUID.randomUUID(), now);

        when(closureRepository.findById(closureId)).thenReturn(Optional.of(closure));
        when(detailRepository.findByMonthlyClosureId(closureId)).thenReturn(List.of());

        MonthlyClosureWithDetailsResult result = service.execute(closureId);

        assertEquals(now, result.createdAt());
        assertTrue(result.details().isEmpty());
    }

    @Test
    void shouldThrowExceptionWhenClosureNotFound() {
        UUID id = UUID.randomUUID();
        when(closureRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(MonthlyClosureNotFoundException.class, () -> service.execute(id));
    }
}
