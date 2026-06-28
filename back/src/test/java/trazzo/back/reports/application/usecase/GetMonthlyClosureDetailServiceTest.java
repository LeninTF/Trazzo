package trazzo.back.reports.application.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import trazzo.back.reports.application.dto.result.MonthlyClosureDetailResult;
import trazzo.back.reports.application.ports.out.MonthlyClosureDetailRepositoryPort;
import trazzo.back.reports.domain.exception.MonthlyClosureDetailNotFoundException;
import trazzo.back.reports.domain.model.closure.MonthlyClosureDetail;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class GetMonthlyClosureDetailServiceTest {

    @Mock
    private MonthlyClosureDetailRepositoryPort detailRepository;

    private GetMonthlyClosureDetailService service;

    @BeforeEach
    void setUp() {
        service = new GetMonthlyClosureDetailService(detailRepository);
    }

    @Test
    void shouldReturnDetailWhenFound() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosureDetail detail = new MonthlyClosureDetail(
                id, UUID.randomUUID(), 1, "Juan Perez", "12345678",
                "TI", "Developer", 160.0, 10, 1, 5.0, now);
        when(detailRepository.findById(id)).thenReturn(Optional.of(detail));

        MonthlyClosureDetailResult result = service.execute(id);
        UUID monthClosureId = detail.getMonthClosureId();

        assertEquals(id, result.id());
        assertEquals(monthClosureId, result.monthClosureId());
        assertEquals("Juan Perez", result.tenantUserFullName());
        assertEquals("12345678", result.tenantUserDocument());
        assertEquals("TI", result.departmentName());
        assertEquals("Developer", result.roleName());
        assertEquals(160.0, result.totalWorkedHours());
        assertEquals(Integer.valueOf(10), result.totalTardinessMinutes());
        assertEquals(1, result.totalAbsences());
        assertEquals(5.0, result.totalOvertimeHours());
        assertEquals(now, result.createdAt());
    }

    @Test
    void shouldThrowExceptionWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(detailRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(MonthlyClosureDetailNotFoundException.class, () -> service.execute(id));
    }
}
