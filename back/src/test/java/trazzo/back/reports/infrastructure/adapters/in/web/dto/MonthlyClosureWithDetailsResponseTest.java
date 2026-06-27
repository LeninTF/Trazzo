package trazzo.back.reports.infrastructure.adapters.in.web.dto;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

class MonthlyClosureWithDetailsResponseTest {

    @Test
    void shouldCreateResponseSuccessfully() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        MonthlyClosureDetailResponse detail = new MonthlyClosureDetailResponse(
                UUID.randomUUID(), UUID.randomUUID(), "Juan", "123", "TI", "Dev", 160.0, 10.0, 1, 5.0, now);
        MonthlyClosureWithDetailsResponse response = new MonthlyClosureWithDetailsResponse(
                id, 6, 2025, 1, "excel", "pdf", now, List.of(detail));
        assertEquals(id, response.id());
        assertEquals(6, response.month());
        assertEquals(2025, response.year());
        assertEquals(1, response.totalEmployees());
        assertEquals("excel", response.excelReportUrl());
        assertEquals("pdf", response.pdfReportUrl());
        assertEquals(now, response.createdAt());
        assertEquals(1, response.details().size());
    }

    @Test
    void shouldCreateResponseWithEmptyDetails() {
        MonthlyClosureWithDetailsResponse response = new MonthlyClosureWithDetailsResponse(
                UUID.randomUUID(), 6, 2025, 0, null, null, LocalDateTime.now(), List.of());
        assertTrue(response.details().isEmpty());
    }

    @Test
    void shouldBeEqualForSameValues() {
        UUID id = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        List<MonthlyClosureDetailResponse> details = List.of();
        MonthlyClosureWithDetailsResponse r1 = new MonthlyClosureWithDetailsResponse(id, 6, 2025, 0, null, null, now, details);
        MonthlyClosureWithDetailsResponse r2 = new MonthlyClosureWithDetailsResponse(id, 6, 2025, 0, null, null, now, details);
        assertEquals(r1, r2);
    }

    @Test
    void shouldReturnToString() {
        MonthlyClosureWithDetailsResponse response = new MonthlyClosureWithDetailsResponse(
                UUID.randomUUID(), 6, 2025, 0, null, null, LocalDateTime.now(), List.of());
        assertNotNull(response.toString());
    }
}
