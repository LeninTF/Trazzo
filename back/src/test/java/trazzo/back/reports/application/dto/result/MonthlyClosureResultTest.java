package trazzo.back.reports.application.dto.result;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;

class MonthlyClosureResultTest {

    @Test
    void shouldCreateResultSuccessfully() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosureResult result = new MonthlyClosureResult(id, 6, 2025, 10, "excel-url", "pdf-url", now);

        assertEquals(id, result.id());
        assertEquals(6, result.month());
        assertEquals(2025, result.year());
        assertEquals(10, result.totalEmployees());
        assertEquals("excel-url", result.excelReportUrl());
        assertEquals("pdf-url", result.pdfReportUrl());
        assertEquals(now, result.createdAt());
    }

    @Test
    void shouldBeEqualForSameValues() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosureResult r1 = new MonthlyClosureResult(id, 6, 2025, 10, "excel", "pdf", now);
        MonthlyClosureResult r2 = new MonthlyClosureResult(id, 6, 2025, 10, "excel", "pdf", now);

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void shouldNotBeEqualForDifferentValues() {
        MonthlyClosureResult r1 = new MonthlyClosureResult(UUID.randomUUID(), 6, 2025, 10, "excel", "pdf", LocalDateTime.now());
        MonthlyClosureResult r2 = new MonthlyClosureResult(UUID.randomUUID(), 7, 2024, 5, "other", "other", LocalDateTime.now());

        assertNotEquals(r1, r2);
    }

    @Test
    void shouldReturnToString() {
        UUID id = UUID.randomUUID();
        MonthlyClosureResult result = new MonthlyClosureResult(id, 6, 2025, 10, "excel", "pdf", LocalDateTime.now());

        assertNotNull(result.toString());
        assertTrue(result.toString().contains(id.toString()));
    }
}
