package trazzo.back.reports.domain.model.closure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDateTime;
import java.util.UUID;

class MonthlyClosureTest {

    private static MonthlyClosure createClosure(UUID id, int month, int year,
                                                  int totalEmployees, String excelUrl,
                                                  String pdfUrl, UUID userId,
                                                  LocalDateTime createdAt) {
        return new MonthlyClosure(id, month, year, totalEmployees, excelUrl, pdfUrl, userId, createdAt);
    }

    @Test
    void shouldCreateMonthlyClosureSuccessfully() {
        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();
        UUID userId = UUID.randomUUID();

        MonthlyClosure closure = createClosure(id, 6, 2025, 100, "excel-url", "pdf-url", userId, createdAt);

        assertEquals(id, closure.getId());
        assertEquals(6, closure.getMonth());
        assertEquals(2025, closure.getYear());
        assertEquals(100, closure.getTotalEmployees());
        assertEquals("excel-url", closure.getExcelReportUrl());
        assertEquals("pdf-url", closure.getPdfReportUrl());
        assertEquals(userId, closure.getCreatedByUserId());
        assertEquals(createdAt, closure.getCreatedAt());
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 13 })
    void shouldThrowExceptionForInvalidMonth(int month) {
        LocalDateTime now = LocalDateTime.now();
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,
                () -> createClosure(id, month, 2025, 10, "excel", "pdf", userId, now));
    }

    @Test
    void shouldThrowExceptionWhenYearIsInvalid() {
        LocalDateTime now = LocalDateTime.now();
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,
                () -> createClosure(id, 6, 1999, 10, "excel", "pdf", userId, now));
    }

    @Test
    void shouldThrowExceptionWhenTotalEmployeesIsNegative() {
        LocalDateTime now = LocalDateTime.now();
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,
                () -> createClosure(id, 6, 2025, -1, "excel", "pdf", userId, now));
    }

    @Test
    void shouldThrowExceptionWhenIdIsNull() {
        LocalDateTime now = LocalDateTime.now();
        UUID userId = UUID.randomUUID();
        assertThrows(NullPointerException.class,
                () -> createClosure(null, 6, 2025, 10, "excel", "pdf", userId, now));
    }

    @Test
    void shouldThrowExceptionWhenCreatedByUserIdIsNull() {
        LocalDateTime now = LocalDateTime.now();
        UUID id = UUID.randomUUID();
        assertThrows(NullPointerException.class,
                () -> createClosure(id, 6, 2025, 10, "excel", "pdf", null, now));
    }

    @Test
    void shouldThrowExceptionWhenCreatedAtIsNull() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        assertThrows(NullPointerException.class,
                () -> createClosure(id, 6, 2025, 10, "excel", "pdf", userId, null));
    }

    @Test
    void shouldCreateNewInstanceWithUpdatedReportUrls() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        MonthlyClosure closure = createClosure(id, 6, 2025, 100, null, null, userId, createdAt);
        MonthlyClosure updated = closure.withReportUrls("new-excel", "new-pdf");

        assertEquals(id, updated.getId());
        assertEquals("new-excel", updated.getExcelReportUrl());
        assertEquals("new-pdf", updated.getPdfReportUrl());
        assertEquals(6, updated.getMonth());
        assertEquals(2025, updated.getYear());
        assertEquals(100, updated.getTotalEmployees());
        assertEquals(userId, updated.getCreatedByUserId());
        assertEquals(createdAt, updated.getCreatedAt());

        assertNull(closure.getExcelReportUrl());
        assertNull(closure.getPdfReportUrl());
    }
}