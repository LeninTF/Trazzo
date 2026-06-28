package trazzo.back.reports.infrastructure.adapters.in.web.dto;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.UUID;

class MonthlyClosureResponseTest {

    @Test
    void shouldCreateResponseSuccessfully() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosureResponse response = new MonthlyClosureResponse(id, 6, 2025, 10, "excel", "pdf", now);
        assertEquals(id, response.id());
        assertEquals(6, response.month());
        assertEquals(2025, response.year());
        assertEquals(10, response.totalEmployees());
        assertEquals("excel", response.excelReportUrl());
        assertEquals("pdf", response.pdfReportUrl());
        assertEquals(now, response.createdAt());
    }

    @Test
    void shouldBeEqualForSameValues() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosureResponse r1 = new MonthlyClosureResponse(id, 6, 2025, 10, "e", "p", now);
        MonthlyClosureResponse r2 = new MonthlyClosureResponse(id, 6, 2025, 10, "e", "p", now);
        assertEquals(r1, r2);
    }

    @Test
    void shouldNotBeEqualForDifferentValues() {
        MonthlyClosureResponse r1 = new MonthlyClosureResponse(UUID.randomUUID(), 6, 2025, 10, "e", "p", LocalDateTime.now());
        MonthlyClosureResponse r2 = new MonthlyClosureResponse(UUID.randomUUID(), 7, 2024, 5, "x", "y", LocalDateTime.now());
        assertNotEquals(r1, r2);
    }

    @Test
    void shouldReturnToString() {
        UUID id = UUID.randomUUID();
        MonthlyClosureResponse response = new MonthlyClosureResponse(id, 6, 2025, 10, "e", "p", LocalDateTime.now());
        assertNotNull(response.toString());
        assertTrue(response.toString().contains(id.toString()));
    }
}
