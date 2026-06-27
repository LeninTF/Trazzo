package trazzo.back.reports.application.dto.result;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

class MonthlyClosureWithDetailsResultTest {

    @Test
    void shouldCreateResultSuccessfully() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosureDetailResult detail = new MonthlyClosureDetailResult(
                UUID.randomUUID(), UUID.randomUUID(), 1, "Juan", "123", "TI", "Dev", 160.0, 10, 1, 5.0, now);
        List<MonthlyClosureDetailResult> details = List.of(detail);

        MonthlyClosureWithDetailsResult result = new MonthlyClosureWithDetailsResult(
                id, 6, 2025, 1, "excel-url", "pdf-url", now, details);

        assertEquals(id, result.id());
        assertEquals(6, result.month());
        assertEquals(2025, result.year());
        assertEquals(1, result.totalEmployees());
        assertEquals("excel-url", result.excelReportUrl());
        assertEquals("pdf-url", result.pdfReportUrl());
        assertEquals(1, result.details().size());
        assertEquals(detail, result.details().getFirst());
        assertEquals(now, result.createdAt());
    }

    @Test
    void shouldCreateResultWithEmptyDetails() {
        MonthlyClosureWithDetailsResult result = new MonthlyClosureWithDetailsResult(
                UUID.randomUUID(), 6, 2025, 0, null, null, LocalDateTime.now(), List.of());

        assertTrue(result.details().isEmpty());
    }

    @Test
    void shouldBeEqualForSameValues() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        List<MonthlyClosureDetailResult> details = List.of();
        MonthlyClosureWithDetailsResult r1 = new MonthlyClosureWithDetailsResult(id, 6, 2025, 0, null, null, now, details);
        MonthlyClosureWithDetailsResult r2 = new MonthlyClosureWithDetailsResult(id, 6, 2025, 0, null, null, now, details);

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void shouldNotBeEqualForDifferentValues() {
        MonthlyClosureWithDetailsResult r1 = new MonthlyClosureWithDetailsResult(
                UUID.randomUUID(), 6, 2025, 0, null, null, LocalDateTime.now(), List.of());
        MonthlyClosureWithDetailsResult r2 = new MonthlyClosureWithDetailsResult(
                UUID.randomUUID(), 7, 2024, 1, "e", "p", LocalDateTime.now(), List.of());

        assertNotEquals(r1, r2);
    }

    @Test
    void shouldReturnToString() {
        MonthlyClosureWithDetailsResult result = new MonthlyClosureWithDetailsResult(
                UUID.randomUUID(), 6, 2025, 0, null, null, LocalDateTime.now(), List.of());

        assertNotNull(result.toString());
    }
}
